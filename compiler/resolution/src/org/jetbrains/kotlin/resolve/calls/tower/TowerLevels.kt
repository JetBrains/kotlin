/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.calls.tower

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.smartcasts.getReceiverValueWithSmartCast
import org.jetbrains.kotlin.resolve.calls.util.FakeCallableDescriptorForObject
import org.jetbrains.kotlin.resolve.coroutine.CoroutineReceiverValue
import org.jetbrains.kotlin.resolve.coroutine.createCoroutineSuspensionFunctionView
import org.jetbrains.kotlin.resolve.descriptorUtil.HIDES_MEMBERS_NAME_LIST
import org.jetbrains.kotlin.resolve.descriptorUtil.hasClassValueDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.hasHidesMembersAnnotation
import org.jetbrains.kotlin.resolve.descriptorUtil.hasLowPriorityInOverloadResolution
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.resolve.scopes.receivers.CastImplicitClassReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.QualifierReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.utils.collectFunctions
import org.jetbrains.kotlin.resolve.scopes.utils.collectVariables
import org.jetbrains.kotlin.resolve.selectMostSpecificInEachOverridableGroup
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.getImmediateSuperclassNotAny
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.*

internal abstract class AbstractScopeTowerLevel(
        protected val scopeTower: ScopeTower
): ScopeTowerLevel {
    protected val location: LookupLocation get() = scopeTower.location

    protected fun <D : CallableDescriptor> createCandidateDescriptor(
            descriptor: D,
            dispatchReceiver: ReceiverValue?,
            specialError: ResolutionDiagnostic? = null,
            dispatchReceiverSmartCastType: KotlinType? = null
    ): CandidateWithBoundDispatchReceiver<D> {
        val diagnostics = SmartList<ResolutionDiagnostic>()
        diagnostics.addIfNotNull(specialError)

        if (ErrorUtils.isError(descriptor)) {
            diagnostics.add(ErrorDescriptorDiagnostic)
        }
        else {
            if (descriptor.hasLowPriorityInOverloadResolution()) diagnostics.add(LowPriorityDescriptorDiagnostic)
            if (descriptor.isSynthesized) diagnostics.add(SynthesizedDescriptorDiagnostic)
            if (dispatchReceiverSmartCastType != null) diagnostics.add(UsedSmartCastForDispatchReceiver(dispatchReceiverSmartCastType))

            val shouldSkipVisibilityCheck = scopeTower.isDebuggerContext
            if (!shouldSkipVisibilityCheck) {
                Visibilities.findInvisibleMember(
                        getReceiverValueWithSmartCast(dispatchReceiver, dispatchReceiverSmartCastType),
                        descriptor,
                        scopeTower.lexicalScope.ownerDescriptor
                )?.let { diagnostics.add(VisibilityError(it)) }
            }
        }
        return CandidateWithBoundDispatchReceiverImpl(dispatchReceiver, descriptor, diagnostics)
    }

}

// todo KT-9538 Unresolved inner class via subclass reference
// todo add static methods & fields with error
internal class ReceiverScopeTowerLevel(
        scopeTower: ScopeTower,
        val dispatchReceiver: ReceiverValue
): AbstractScopeTowerLevel(scopeTower) {

    private fun <D : CallableDescriptor> collectMembers(
            getMembers: ResolutionScope.(KotlinType?) -> Collection<D>
    ): Collection<CandidateWithBoundDispatchReceiver<D>> {
        val result = ArrayList<CandidateWithBoundDispatchReceiver<D>>(0)
        dispatchReceiver.type.memberScope.getMembers(dispatchReceiver.type).mapTo(result) {
            createCandidateDescriptor(it, dispatchReceiver)
        }

        val smartCastPossibleTypes = scopeTower.dataFlowInfo.getSmartCastTypes(dispatchReceiver)
        val unstableError = if (scopeTower.dataFlowInfo.isStableReceiver(dispatchReceiver)) null else UnstableSmartCastDiagnostic
        val unstableCandidates = if (unstableError != null) ArrayList<CandidateWithBoundDispatchReceiver<D>>(0) else null

        for (possibleType in smartCastPossibleTypes) {
            possibleType.memberScope.getMembers(possibleType).mapTo(unstableCandidates ?: result) {
                createCandidateDescriptor(it, dispatchReceiver.smartCastReceiver(possibleType), unstableError, dispatchReceiverSmartCastType = possibleType)
            }
        }

        if (smartCastPossibleTypes.isNotEmpty()) {
            if (unstableCandidates == null) {
                result.retainAll(result.selectMostSpecificInEachOverridableGroup { descriptor })
            }
            else {
                result.addAll(unstableCandidates.selectMostSpecificInEachOverridableGroup { descriptor })
            }
        }

        if (dispatchReceiver.type.isDynamic()) {
            scopeTower.dynamicScope.getMembers(null).mapTo(result) {
                createCandidateDescriptor(it, dispatchReceiver, DynamicDescriptorDiagnostic)
            }
        }

        if (dispatchReceiver is CoroutineReceiverValue) {
            result.addAll(result.mapNotNull {
                val realDescriptor = it.descriptor
                val suspensionFunctionView =
                        (realDescriptor as? SimpleFunctionDescriptor)?.createCoroutineSuspensionFunctionView() ?: return@mapNotNull null
                @Suppress("UNCHECKED_CAST")
                createCandidateDescriptor(suspensionFunctionView as D, dispatchReceiver)
            })
        }

        return result
    }

    private fun ReceiverValue.smartCastReceiver(targetType: KotlinType)
            = if (this is ImplicitClassReceiver) CastImplicitClassReceiver(this.classDescriptor, targetType) else this

    override fun getVariables(name: Name, extensionReceiver: ReceiverValue?): Collection<CandidateWithBoundDispatchReceiver<VariableDescriptor>> {
        return collectMembers { getContributedVariables(name, location) }
    }

    override fun getObjects(name: Name, extensionReceiver: ReceiverValue?): Collection<CandidateWithBoundDispatchReceiver<VariableDescriptor>> {
        return emptyList()
    }

    override fun getFunctions(name: Name, extensionReceiver: ReceiverValue?): Collection<CandidateWithBoundDispatchReceiver<FunctionDescriptor>> {
        return collectMembers {
            getContributedFunctions(name, location) + it.getInnerConstructors(name, location)
        }
    }
}

internal class QualifierScopeTowerLevel(scopeTower: ScopeTower, val qualifier: QualifierReceiver) : AbstractScopeTowerLevel(scopeTower) {
    override fun getVariables(name: Name, extensionReceiver: ReceiverValue?) = qualifier.staticScope
            .getContributedVariables(name, location).map {
                createCandidateDescriptor(it, dispatchReceiver = null)
            }

    override fun getObjects(name: Name, extensionReceiver: ReceiverValue?) = qualifier.staticScope
            .getContributedObjectVariables(name, location).map {
                createCandidateDescriptor(it, dispatchReceiver = null)
            }

    override fun getFunctions(name: Name, extensionReceiver: ReceiverValue?) = qualifier.staticScope
            .getContributedFunctionsAndConstructors(name, location).map {
                createCandidateDescriptor(it, dispatchReceiver = null)
            }
}

// KT-3335 Creating imported super class' inner class fails in codegen
internal open class ScopeBasedTowerLevel protected constructor(
        scopeTower: ScopeTower,
        private val resolutionScope: ResolutionScope
) : AbstractScopeTowerLevel(scopeTower) {

    internal constructor(scopeTower: ScopeTower, lexicalScope: LexicalScope): this(scopeTower, lexicalScope as ResolutionScope)

    override fun getVariables(name: Name, extensionReceiver: ReceiverValue?): Collection<CandidateWithBoundDispatchReceiver<VariableDescriptor>>
            = resolutionScope.getContributedVariables(name, location).map {
                createCandidateDescriptor(it, dispatchReceiver = null)
            }

    override fun getObjects(name: Name, extensionReceiver: ReceiverValue?): Collection<CandidateWithBoundDispatchReceiver<VariableDescriptor>>
            = resolutionScope.getContributedObjectVariables(name, location).map {
                createCandidateDescriptor(it, dispatchReceiver = null)
            }

    override fun getFunctions(name: Name, extensionReceiver: ReceiverValue?): Collection<CandidateWithBoundDispatchReceiver<FunctionDescriptor>>
            = resolutionScope.getContributedFunctionsAndConstructors(name, location).map {
                createCandidateDescriptor(it, dispatchReceiver = null)
            }
}
internal class ImportingScopeBasedTowerLevel(
        scopeTower: ScopeTower,
        importingScope: ImportingScope
): ScopeBasedTowerLevel(scopeTower, importingScope)

internal class SyntheticScopeBasedTowerLevel(
        scopeTower: ScopeTower,
        private val syntheticScopes: SyntheticScopes
): AbstractScopeTowerLevel(scopeTower) {
    override fun getVariables(name: Name, extensionReceiver: ReceiverValue?): Collection<CandidateWithBoundDispatchReceiver<VariableDescriptor>> {
        if (extensionReceiver == null) return emptyList()

        val extensionReceiverTypes = scopeTower.dataFlowInfo.getAllPossibleTypes(extensionReceiver)
        return syntheticScopes.collectSyntheticExtensionProperties(extensionReceiverTypes, name, location).map {
            createCandidateDescriptor(it, dispatchReceiver = null)
        }
    }

    override fun getObjects(name: Name, extensionReceiver: ReceiverValue?): Collection<CandidateWithBoundDispatchReceiver<VariableDescriptor>>
            = emptyList()

    override fun getFunctions(name: Name, extensionReceiver: ReceiverValue?): Collection<CandidateWithBoundDispatchReceiver<FunctionDescriptor>> {
        if (extensionReceiver == null) return emptyList()

        val extensionReceiverTypes = scopeTower.dataFlowInfo.getAllPossibleTypes(extensionReceiver)
        return syntheticScopes.collectSyntheticExtensionFunctions(extensionReceiverTypes, name, location).map {
            createCandidateDescriptor(it, dispatchReceiver = null)
        }
    }
}

internal class HidesMembersTowerLevel(scopeTower: ScopeTower): AbstractScopeTowerLevel(scopeTower) {
    override fun getVariables(name: Name, extensionReceiver: ReceiverValue?)
            = getCandidates(name, extensionReceiver, LexicalScope::collectVariables)

    override fun getObjects(name: Name, extensionReceiver: ReceiverValue?)
            = emptyList<CandidateWithBoundDispatchReceiver<VariableDescriptor>>()

    override fun getFunctions(name: Name, extensionReceiver: ReceiverValue?)
            = getCandidates(name, extensionReceiver, LexicalScope::collectFunctions)

    private fun <T: CallableDescriptor> getCandidates(
            name: Name,
            extensionReceiver: ReceiverValue?,
            collectCandidates: LexicalScope.(Name, LookupLocation) -> Collection<T>
    ): Collection<CandidateWithBoundDispatchReceiver<T>> {
        if (extensionReceiver == null || name !in HIDES_MEMBERS_NAME_LIST) return emptyList()

        return scopeTower.lexicalScope.collectCandidates(name, location).filter {
            it.extensionReceiverParameter != null && it.hasHidesMembersAnnotation()
        }.map {
            createCandidateDescriptor(it, dispatchReceiver = null)
        }
    }
}

private fun KotlinType.getClassifierFromMeAndSuperclasses(name: Name, location: LookupLocation): ClassifierDescriptor? {
    var superclass: KotlinType? = this
    while (superclass != null) {
        superclass.memberScope.getContributedClassifier(name, location)?.let { return it }
        superclass = superclass.getImmediateSuperclassNotAny()
    }
    return null
}

private fun KotlinType?.getInnerConstructors(name: Name, location: LookupLocation): Collection<FunctionDescriptor> {
    val classifierDescriptor = getClassWithConstructors(this?.getClassifierFromMeAndSuperclasses(name, location))
    return classifierDescriptor?.constructors?.filter { it.dispatchReceiverParameter != null } ?: emptyList()
}

private fun ResolutionScope.getContributedFunctionsAndConstructors(name: Name, location: LookupLocation): Collection<FunctionDescriptor> {
    val classifier = getContributedClassifier(name, location)
    return getContributedFunctions(name, location) +
           (getClassWithConstructors(classifier)?.constructors?.filter { it.dispatchReceiverParameter == null } ?: emptyList()) +
           (classifier?.getTypeAliasConstructors() ?: emptyList())
}

private fun ResolutionScope.getContributedObjectVariables(name: Name, location: LookupLocation): Collection<VariableDescriptor> {
    val objectDescriptor = getFakeDescriptorForObject(getContributedClassifier(name, location))
    return listOfNotNull(objectDescriptor)
}

private fun getFakeDescriptorForObject(classifier: ClassifierDescriptor?): FakeCallableDescriptorForObject? =
        when (classifier) {
            is TypeAliasDescriptor ->
                getFakeDescriptorForObject(classifier.classDescriptor)
            is ClassDescriptor ->
                if (classifier.hasClassValueDescriptor)
                    FakeCallableDescriptorForObject(classifier)
                else
                    null
            else -> null
        }

private fun getClassWithConstructors(classifier: ClassifierDescriptor?): ClassDescriptor? =
        if (classifier !is ClassDescriptor || !classifier.canHaveCallableConstructors)
            null
        else
            classifier

private val ClassDescriptor.canHaveCallableConstructors: Boolean
    get() = !ErrorUtils.isError(this) && !kind.isSingleton

fun ClassifierDescriptor.getTypeAliasConstructors(): Collection<ConstructorDescriptor> {
    if (this !is TypeAliasDescriptor) return emptyList()

    val classDescriptor = this.classDescriptor ?: return emptyList()
    if (!classDescriptor.canHaveCallableConstructors) return emptyList()

    val substitutor = this.getTypeSubstitutorForUnderlyingClass() ?: throw AssertionError("classDescriptor should be non-null for $this")

    return classDescriptor.constructors.mapNotNull {
        if (it.dispatchReceiverParameter == null)
            TypeAliasConstructorDescriptorImpl.create(this, it, substitutor)
        else
            null
    }
}

private fun TypeAliasDescriptor.getTypeSubstitutorForUnderlyingClass(): TypeSubstitutor? {
    if (classDescriptor == null) return null

    val expandedTypeParameters = expandedType.constructor.parameters
    val expandedTypeArguments = expandedType.arguments
    return TypeSubstitutor.create(IndexedParametersSubstitution(expandedTypeParameters, expandedTypeArguments))
}
