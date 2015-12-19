/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.resolve.calls.tasks.collectors

import com.intellij.util.SmartList
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.functions.FunctionInvokeDescriptor
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils.isStaticNestedClass
import org.jetbrains.kotlin.resolve.LibrarySourceHacks
import org.jetbrains.kotlin.resolve.calls.tasks.createSynthesizedInvokes
import org.jetbrains.kotlin.resolve.calls.util.FakeCallableDescriptorForObject
import org.jetbrains.kotlin.resolve.descriptorUtil.hasClassValueDescriptor
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.resolve.scopes.utils.*
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addIfNotNull

public interface CallableDescriptorCollector<D : CallableDescriptor> {

    public fun getLocalNonExtensionsByName(lexicalScope: LexicalScope, name: Name, location: LookupLocation): Collection<D>

    public fun getNonExtensionsByName(scope: HierarchicalScope, name: Name, location: LookupLocation): Collection<D>

    // todo this is hack for static members priority
    public fun getStaticInheritanceByName(lexicalScope: LexicalScope, name: Name, location: LookupLocation): Collection<D>

    public fun getMembersByName(receiver: KotlinType, name: Name, location: LookupLocation): Collection<D>

    public fun getStaticMembersByName(receiver: KotlinType, name: Name, location: LookupLocation): Collection<D>

    public fun getExtensionsByName(scope: HierarchicalScope, syntheticScopes: SyntheticScopes, name: Name, receiverTypes: Collection<KotlinType>, location: LookupLocation): Collection<D>
}

private fun <D : CallableDescriptor> CallableDescriptorCollector<D>.withDefaultFilter() = filtered { !LibrarySourceHacks.shouldSkip(it) }

private val FUNCTIONS_COLLECTOR = FunctionCollector.withDefaultFilter()
private val VARIABLES_COLLECTOR = VariableCollector.withDefaultFilter()

public class CallableDescriptorCollectors<D : CallableDescriptor>(val collectors: List<CallableDescriptorCollector<D>>) :
        Iterable<CallableDescriptorCollector<D>> {
    override fun iterator(): Iterator<CallableDescriptorCollector<D>> = collectors.iterator()

    @Suppress("UNCHECKED_CAST")
    companion object {
        public val FUNCTIONS_AND_VARIABLES: CallableDescriptorCollectors<CallableDescriptor> =
                CallableDescriptorCollectors(listOf(
                        FUNCTIONS_COLLECTOR as CallableDescriptorCollector<CallableDescriptor>,
                        VARIABLES_COLLECTOR as CallableDescriptorCollector<CallableDescriptor>
                ))
        public val FUNCTIONS: CallableDescriptorCollectors<CallableDescriptor> =
                CallableDescriptorCollectors(listOf(FUNCTIONS_COLLECTOR as CallableDescriptorCollector<CallableDescriptor>))
        public val VARIABLES: CallableDescriptorCollectors<VariableDescriptor> = CallableDescriptorCollectors(listOf(VARIABLES_COLLECTOR))
    }
}

public fun <D : CallableDescriptor> CallableDescriptorCollectors<D>.filtered(filter: (D) -> Boolean): CallableDescriptorCollectors<D> =
        CallableDescriptorCollectors(this.collectors.map { it.filtered(filter) })

private object FunctionCollector : CallableDescriptorCollector<FunctionDescriptor> {
    override fun getStaticInheritanceByName(lexicalScope: LexicalScope, name: Name, location: LookupLocation): Collection<FunctionDescriptor> {
        return lexicalScope.collectAllFromMeAndParent {
            if (it is LexicalChainedScope && it.isStaticScope) {
                it.getContributedFunctions(name, location).filter { it.extensionReceiverParameter == null }
            }
            else {
                emptyList()
            }
        }
    }

    override fun getLocalNonExtensionsByName(lexicalScope: LexicalScope, name: Name, location: LookupLocation): Collection<FunctionDescriptor> {
        return lexicalScope.collectAllFromMeAndParent {
            if (it is LexicalScope && it.ownerDescriptor is FunctionDescriptor) {
                it.getContributedFunctions(name, location).filter { it.extensionReceiverParameter == null } +
                    getConstructors(it.getContributedClassifier(name, location))
            }
            else {
                emptyList()
            }
        }
    }

    override fun getNonExtensionsByName(scope: HierarchicalScope, name: Name, location: LookupLocation): Collection<FunctionDescriptor> {
        return scope.collectFunctions(name, location).filter { it.extensionReceiverParameter == null } + getConstructors(scope, name, location)
    }

    override fun getMembersByName(receiver: KotlinType, name: Name, location: LookupLocation): Collection<FunctionDescriptor> {
        val receiverScope = receiver.memberScope
        val members = receiverScope.getContributedFunctions(name, location)
        val constructors = getConstructors(receiverScope.memberScopeAsImportingScope(), name, location, { !isStaticNestedClass(it) })

        if (name == OperatorNameConventions.INVOKE && KotlinBuiltIns.isExtensionFunctionType(receiver)) {
            // If we're looking for members of an extension function type, we ignore the non-extension "invoke"s
            // that originate from the Function{n} class and only consider the synthesized "invoke" extensions.
            // Otherwise confusing errors will be reported because the non-extension here beats the extension
            // (because declarations beat synthesized members)
            val (candidatesForReplacement, irrelevantInvokes) =
                    members.partition { it is FunctionInvokeDescriptor && it.valueParameters.isNotEmpty() }
            return createSynthesizedInvokes(candidatesForReplacement) + candidatesForReplacement + irrelevantInvokes + constructors
        }

        return members + constructors
    }

    override fun getStaticMembersByName(receiver: KotlinType, name: Name, location: LookupLocation): Collection<FunctionDescriptor> {
        return getConstructors(receiver.memberScope.memberScopeAsImportingScope(), name, location, { isStaticNestedClass(it) })
    }

    override fun getExtensionsByName(scope: HierarchicalScope, syntheticScopes: SyntheticScopes, name: Name, receiverTypes: Collection<KotlinType>, location: LookupLocation): Collection<FunctionDescriptor> {
        val functions = scope.collectFunctions(name, location)
        val (extensions, nonExtensions) = functions.partition { it.extensionReceiverParameter != null }
        val syntheticExtensions = syntheticScopes.collectSyntheticExtensionFunctions(receiverTypes, name, location)

        if (name == OperatorNameConventions.INVOKE) {
            // Create synthesized "invoke" extensions for each non-extension "invoke" found in the scope
            return extensions + createSynthesizedInvokes(nonExtensions) + syntheticExtensions
        }

        return extensions + syntheticExtensions
    }

    private fun getConstructors(
            scope: HierarchicalScope,
            name: Name,
            location: LookupLocation,
            filterClassPredicate: (ClassDescriptor) -> Boolean = { true }
    ): Collection<FunctionDescriptor> {
        val classifier = scope.findClassifier(name, location)
        return getConstructors(classifier, filterClassPredicate)
    }

    private fun getConstructors(
            classifier: ClassifierDescriptor?,
            filterClassPredicate: (ClassDescriptor) -> Boolean = { true }
    ): Collection<FunctionDescriptor> {
        if (classifier !is ClassDescriptor || ErrorUtils.isError(classifier) || !filterClassPredicate(classifier)
            // Constructors of singletons shouldn't be callable from the code
            || classifier.kind.isSingleton) {
            return listOf()
        }
        return classifier.constructors
    }

    override fun toString() = "FUNCTIONS"
}

private object VariableCollector : CallableDescriptorCollector<VariableDescriptor> {
    override fun getLocalNonExtensionsByName(lexicalScope: LexicalScope, name: Name, location: LookupLocation): Collection<VariableDescriptor> {
        val result = SmartList<VariableDescriptor>()
        result.addIfNotNull(lexicalScope.findLocalVariable(name))
        // Although local objects are prohibited, we'll include objects declared in current scope so that their usages are still resolved.
        result.addIfNotNull(getContributedFakeDescriptorForObject(lexicalScope, name, location))
        return result
    }

    override fun getStaticInheritanceByName(lexicalScope: LexicalScope, name: Name, location: LookupLocation): Collection<VariableDescriptor> {
        return lexicalScope.collectAllFromMeAndParent {
            if (it is LexicalChainedScope && it.isStaticScope) {
                it.getContributedVariables(name, location)
            }
            else {
                emptyList()
            }
        }
    }

    private fun getContributedFakeDescriptorForObject(scope: LexicalScope, name: Name, location: LookupLocation): VariableDescriptor? {
        val classifier = scope.getContributedClassifier(name, location)
        if (classifier !is ClassDescriptor || !classifier.hasClassValueDescriptor) return null
        return FakeCallableDescriptorForObject(classifier)
    }

    private fun findFakeDescriptorForObject(scope: HierarchicalScope, name: Name, location: LookupLocation): VariableDescriptor? {
        val classifier = scope.findClassifier(name, location)
        if (classifier !is ClassDescriptor || !classifier.hasClassValueDescriptor) return null

        return FakeCallableDescriptorForObject(classifier)
    }

    override fun getNonExtensionsByName(scope: HierarchicalScope, name: Name, location: LookupLocation): Collection<VariableDescriptor> {
        val properties = scope.collectVariables(name, location).filter { it.extensionReceiverParameter == null }
        val fakeDescriptor = findFakeDescriptorForObject(scope, name, location)
        return if (fakeDescriptor != null) properties + fakeDescriptor else properties
    }

    override fun getMembersByName(receiver: KotlinType, name: Name, location: LookupLocation): Collection<VariableDescriptor> {
        val memberScope = receiver.memberScope
        val properties = memberScope.getContributedVariables(name, location)
        val fakeDescriptor = findFakeDescriptorForObject(memberScope.memberScopeAsImportingScope(), name, location)
        return if (fakeDescriptor != null) properties + fakeDescriptor else properties
    }

    override fun getStaticMembersByName(receiver: KotlinType, name: Name, location: LookupLocation): Collection<VariableDescriptor> {
        return listOf()
    }

    override fun getExtensionsByName(scope: HierarchicalScope, syntheticScopes: SyntheticScopes, name: Name, receiverTypes: Collection<KotlinType>, location: LookupLocation): Collection<VariableDescriptor> {
        // property may have an extension function type, we check the applicability later to avoid an early computing of deferred types
        return scope.collectVariables(name, location) +
               syntheticScopes.collectSyntheticExtensionProperties(receiverTypes, name, location)
    }

    override fun toString() = "VARIABLES"
}

private fun <D : CallableDescriptor> CallableDescriptorCollector<D>.filtered(filter: (D) -> Boolean): CallableDescriptorCollector<D> {
    val delegate = this
    return object : CallableDescriptorCollector<D> {
        override fun getLocalNonExtensionsByName(lexicalScope: LexicalScope, name: Name, location: LookupLocation): Collection<D> {
            return delegate.getLocalNonExtensionsByName(lexicalScope, name, location).filter(filter)
        }

        override fun getStaticInheritanceByName(lexicalScope: LexicalScope, name: Name, location: LookupLocation): Collection<D> {
            return delegate.getStaticInheritanceByName(lexicalScope, name, location)
        }

        override fun getNonExtensionsByName(scope: HierarchicalScope, name: Name, location: LookupLocation): Collection<D> {
            return delegate.getNonExtensionsByName(scope, name, location).filter(filter)
        }

        override fun getMembersByName(receiver: KotlinType, name: Name, location: LookupLocation): Collection<D> {
            return delegate.getMembersByName(receiver, name, location).filter(filter)
        }

        override fun getStaticMembersByName(receiver: KotlinType, name: Name, location: LookupLocation): Collection<D> {
            return delegate.getStaticMembersByName(receiver, name, location).filter(filter)
        }

        override fun getExtensionsByName(scope: HierarchicalScope, syntheticScopes: SyntheticScopes, name: Name, receiverTypes: Collection<KotlinType>, location: LookupLocation): Collection<D> {
            return delegate.getExtensionsByName(scope, syntheticScopes, name, receiverTypes, location).filter(filter)
        }

        override fun toString(): String {
            return delegate.toString()
        }
    }
}
