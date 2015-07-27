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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.functions.FunctionInvokeDescriptor
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.DescriptorUtils.isStaticNestedClass
import org.jetbrains.kotlin.resolve.LibrarySourceHacks
import org.jetbrains.kotlin.resolve.calls.tasks.createSynthesizedInvokes
import org.jetbrains.kotlin.resolve.calls.util.FakeCallableDescriptorForObject
import org.jetbrains.kotlin.resolve.descriptorUtil.hasClassObjectType
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.utils.singletonOrEmptyList

public interface CallableDescriptorCollector<D : CallableDescriptor> {

    public fun getNonExtensionsByName(scope: JetScope, name: Name, bindingTrace: BindingTrace): Collection<D>

    public fun getMembersByName(receiver: JetType, name: Name, bindingTrace: BindingTrace): Collection<D>

    public fun getStaticMembersByName(receiver: JetType, name: Name, bindingTrace: BindingTrace): Collection<D>

    public fun getExtensionsByName(scope: JetScope, name: Name, receiverTypes: Collection<JetType>, bindingTrace: BindingTrace): Collection<D>
}

private fun <D : CallableDescriptor> CallableDescriptorCollector<D>.withDefaultFilter() = filtered { !LibrarySourceHacks.shouldSkip(it) }

private val FUNCTIONS_COLLECTOR = FunctionCollector.withDefaultFilter()
private val VARIABLES_COLLECTOR = VariableCollector.withDefaultFilter()
private val PROPERTIES_COLLECTOR = PropertyCollector.withDefaultFilter()

public class CallableDescriptorCollectors<D : CallableDescriptor>(val collectors: List<CallableDescriptorCollector<D>>) :
        Iterable<CallableDescriptorCollector<D>> {
    override fun iterator(): Iterator<CallableDescriptorCollector<D>> = collectors.iterator()

    @suppress("UNCHECKED_CAST")
    companion object {
        public val FUNCTIONS_AND_VARIABLES: CallableDescriptorCollectors<CallableDescriptor> =
                CallableDescriptorCollectors(listOf(
                        FUNCTIONS_COLLECTOR as CallableDescriptorCollector<CallableDescriptor>,
                        VARIABLES_COLLECTOR as CallableDescriptorCollector<CallableDescriptor>
                ))
        public val FUNCTIONS: CallableDescriptorCollectors<CallableDescriptor> =
                CallableDescriptorCollectors(listOf(FUNCTIONS_COLLECTOR as CallableDescriptorCollector<CallableDescriptor>))
        public val VARIABLES: CallableDescriptorCollectors<VariableDescriptor> = CallableDescriptorCollectors(listOf(VARIABLES_COLLECTOR))
        public val PROPERTIES: CallableDescriptorCollectors<VariableDescriptor> = CallableDescriptorCollectors(listOf(PROPERTIES_COLLECTOR))
    }
}

public fun <D : CallableDescriptor> CallableDescriptorCollectors<D>.filtered(filter: (D) -> Boolean): CallableDescriptorCollectors<D> =
        CallableDescriptorCollectors(this.collectors.map { it.filtered(filter) })

private object FunctionCollector : CallableDescriptorCollector<FunctionDescriptor> {

    override fun getNonExtensionsByName(scope: JetScope, name: Name, bindingTrace: BindingTrace): Collection<FunctionDescriptor> {
        return scope.getFunctions(name).filter { it.getExtensionReceiverParameter() == null } + getConstructors(scope, name)
    }

    override fun getMembersByName(receiver: JetType, name: Name, bindingTrace: BindingTrace): Collection<FunctionDescriptor> {
        val receiverScope = receiver.getMemberScope()
        val members = receiverScope.getFunctions(name)
        val constructors = getConstructors(receiverScope, name, { !isStaticNestedClass(it) })

        if (name == OperatorConventions.INVOKE && KotlinBuiltIns.isExtensionFunctionType(receiver)) {
            // If we're looking for members of an extension function type, we ignore the non-extension "invoke"s
            // that originate from the Function{n} class and only consider the synthesized "invoke" extensions.
            // Otherwise confusing errors will be reported because the non-extension here beats the extension
            // (because declarations beat synthesized members)
            val (candidatesForReplacement, irrelevantInvokes) =
                    members.partition { it is FunctionInvokeDescriptor && it.getValueParameters().isNotEmpty() }
            return createSynthesizedInvokes(candidatesForReplacement) + irrelevantInvokes + constructors
        }

        return members + constructors
    }

    override fun getStaticMembersByName(receiver: JetType, name: Name, bindingTrace: BindingTrace): Collection<FunctionDescriptor> {
        return getConstructors(receiver.getMemberScope(), name, { isStaticNestedClass(it) })
    }

    override fun getExtensionsByName(scope: JetScope, name: Name, receiverTypes: Collection<JetType>, bindingTrace: BindingTrace): Collection<FunctionDescriptor> {
        val functions = scope.getFunctions(name)
        val (extensions, nonExtensions) = functions.partition { it.getExtensionReceiverParameter() != null }

        if (name == OperatorConventions.INVOKE) {
            // Create synthesized "invoke" extensions for each non-extension "invoke" found in the scope
            return extensions + createSynthesizedInvokes(nonExtensions)
        }

        return extensions
    }

    private fun getConstructors(
            scope: JetScope, name: Name, filterClassPredicate: (ClassDescriptor) -> Boolean = { true }
    ): Collection<FunctionDescriptor> {
        val classifier = scope.getClassifier(name)
        if (classifier !is ClassDescriptor || ErrorUtils.isError(classifier) || !filterClassPredicate(classifier)
            // Constructors of singletons shouldn't be callable from the code
            || classifier.getKind().isSingleton()) {
            return listOf()
        }
        return classifier.getConstructors()
    }

    override fun toString() = "FUNCTIONS"
}

private object VariableCollector : CallableDescriptorCollector<VariableDescriptor> {

    private fun getFakeDescriptorForObject(scope: JetScope, name: Name): VariableDescriptor? {
        val classifier = scope.getClassifier(name)
        if (classifier !is ClassDescriptor || !classifier.hasClassObjectType) return null

        return FakeCallableDescriptorForObject(classifier)
    }

    override fun getNonExtensionsByName(scope: JetScope, name: Name, bindingTrace: BindingTrace): Collection<VariableDescriptor> {
        val localVariable = scope.getLocalVariable(name)
        if (localVariable != null) {
            return setOf(localVariable)
        }
        val properties = scope.getProperties(name).filter { it.extensionReceiverParameter == null }
        val fakeDescriptor = getFakeDescriptorForObject(scope, name)
        return if (fakeDescriptor != null) properties + fakeDescriptor else properties
    }

    override fun getMembersByName(receiver: JetType, name: Name, bindingTrace: BindingTrace): Collection<VariableDescriptor> {
        val memberScope = receiver.memberScope
        val properties = memberScope.getProperties(name)
        val fakeDescriptor = getFakeDescriptorForObject(memberScope, name)
        return if (fakeDescriptor != null) properties + fakeDescriptor else properties
    }

    override fun getStaticMembersByName(receiver: JetType, name: Name, bindingTrace: BindingTrace): Collection<VariableDescriptor> {
        return listOf()
    }

    override fun getExtensionsByName(scope: JetScope, name: Name, receiverTypes: Collection<JetType>, bindingTrace: BindingTrace): Collection<VariableDescriptor> {
        // property may have an extension function type, we check the applicability later to avoid an early computing of deferred types
        return scope.getLocalVariable(name).singletonOrEmptyList() + scope.getProperties(name) + scope.getSyntheticExtensionProperties(receiverTypes, name)
    }

    override fun toString() = "VARIABLES"
}

private object PropertyCollector : CallableDescriptorCollector<VariableDescriptor> {
    private fun filterProperties(variableDescriptors: Collection<VariableDescriptor>) =
            variableDescriptors.filter { it is PropertyDescriptor }

    override fun getNonExtensionsByName(scope: JetScope, name: Name, bindingTrace: BindingTrace): Collection<VariableDescriptor> {
        return filterProperties(VARIABLES_COLLECTOR.getNonExtensionsByName(scope, name, bindingTrace))
    }

    override fun getMembersByName(receiver: JetType, name: Name, bindingTrace: BindingTrace): Collection<VariableDescriptor> {
        return filterProperties(VARIABLES_COLLECTOR.getMembersByName(receiver, name, bindingTrace))
    }

    override fun getStaticMembersByName(receiver: JetType, name: Name, bindingTrace: BindingTrace): Collection<VariableDescriptor> {
        return filterProperties(VARIABLES_COLLECTOR.getStaticMembersByName(receiver, name, bindingTrace))
    }

    override fun getExtensionsByName(scope: JetScope, name: Name, receiverTypes: Collection<JetType>, bindingTrace: BindingTrace): Collection<VariableDescriptor> {
        return filterProperties(VARIABLES_COLLECTOR.getExtensionsByName(scope, name, receiverTypes, bindingTrace))
    }

    override fun toString() = "PROPERTIES"
}

private fun <D : CallableDescriptor> CallableDescriptorCollector<D>.filtered(filter: (D) -> Boolean): CallableDescriptorCollector<D> {
    val delegate = this
    return object : CallableDescriptorCollector<D> {
        override fun getNonExtensionsByName(scope: JetScope, name: Name, bindingTrace: BindingTrace): Collection<D> {
            return delegate.getNonExtensionsByName(scope, name, bindingTrace).filter(filter)
        }

        override fun getMembersByName(receiver: JetType, name: Name, bindingTrace: BindingTrace): Collection<D> {
            return delegate.getMembersByName(receiver, name, bindingTrace).filter(filter)
        }

        override fun getStaticMembersByName(receiver: JetType, name: Name, bindingTrace: BindingTrace): Collection<D> {
            return delegate.getStaticMembersByName(receiver, name, bindingTrace).filter(filter)
        }

        override fun getExtensionsByName(scope: JetScope, name: Name, receiverTypes: Collection<JetType>, bindingTrace: BindingTrace): Collection<D> {
            return delegate.getExtensionsByName(scope, name, receiverTypes, bindingTrace).filter(filter)
        }

        override fun toString(): String {
            return delegate.toString()
        }
    }
}
