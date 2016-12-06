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
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.smartcasts.getReceiverValueWithSmartCast
import org.jetbrains.kotlin.resolve.calls.util.FakeCallableDescriptorForObject
import org.jetbrains.kotlin.resolve.calls.util.FakeCallableDescriptorForTypeAliasObject
import org.jetbrains.kotlin.resolve.descriptorUtil.HIDES_MEMBERS_NAME_LIST
import org.jetbrains.kotlin.resolve.descriptorUtil.hasClassValueDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.hasHidesMembersAnnotation
import org.jetbrains.kotlin.resolve.descriptorUtil.hasLowPriorityInOverloadResolution
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.resolve.scopes.receivers.CastImplicitClassReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.QualifierReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import org.jetbrains.kotlin.resolve.scopes.utils.collectFunctions
import org.jetbrains.kotlin.resolve.scopes.utils.collectVariables
import org.jetbrains.kotlin.resolve.selectMostSpecificInEachOverridableGroup
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.getImmediateSuperclassNotAny
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.singletonOrEmptyList
import org.jetbrains.kotlin.utils.toReadOnlyList
import java.util.*

internal abstract class AbstractScopeTowerLevel(
        protected val scopeTower: ImplicitScopeTower
): ScopeTowerLevel {
    protected val location: LookupLocation get() = scopeTower.location

    protected fun <D : CallableDescriptor> createCandidateDescriptor(
            descriptor: D,
            dispatchReceiver: ReceiverValueWithSmartCastInfo?,
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
            if (dispatchReceiverSmartCastType != null) diagnostics.add(UsedSmartCastForDispatchReceiver(dispatchReceiverSmartCastType))

            val shouldSkipVisibilityCheck = scopeTower.isDebuggerContext
            if (!shouldSkipVisibilityCheck) {
                Visibilities.findInvisibleMember(
                        getReceiverValueWithSmartCast(dispatchReceiver?.receiverValue, dispatchReceiverSmartCastType),
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
internal class MemberScopeTowerLevel(
        scopeTower: ImplicitScopeTower,
        val dispatchReceiver: ReceiverValueWithSmartCastInfo
): AbstractScopeTowerLevel(scopeTower) {

    private val syntheticScopes = scopeTower.syntheticScopes

    private fun <D : CallableDescriptor> collectMembers(
            getMembers: ResolutionScope.(KotlinType?) -> Collection<D>
    ): Collection<CandidateWithBoundDispatchReceiver<D>> {
        val result = ArrayList<CandidateWithBoundDispatchReceiver<D>>(0)
        val receiverValue = dispatchReceiver.receiverValue
        receiverValue.type.memberScope.getMembers(receiverValue.type).mapTo(result) {
            createCandidateDescriptor(it, dispatchReceiver)
        }

        val unstableError = if (dispatchReceiver.isStable) null else UnstableSmartCastDiagnostic
        val unstableCandidates = if (unstableError != null) ArrayList<CandidateWithBoundDispatchReceiver<D>>(0) else null

        for (possibleType in dispatchReceiver.possibleTypes) {
            possibleType.memberScope.getMembers(possibleType).mapTo(unstableCandidates ?: result) {
                createCandidateDescriptor(
                        it,
                        dispatchReceiver.smartCastReceiver(possibleType),
                        unstableError, dispatchReceiverSmartCastType = possibleType
                )
            }
        }

        if (dispatchReceiver.possibleTypes.isNotEmpty()) {
            if (unstableCandidates == null) {
                result.retainAll(result.selectMostSpecificInEachOverridableGroup { descriptor })
            }
            else {
                result.addAll(unstableCandidates.selectMostSpecificInEachOverridableGroup { descriptor })
            }
        }

        if (receiverValue.type.isDynamic()) {
            scopeTower.dynamicScope.getMembers(null).mapTo(result) {
                createCandidateDescriptor(it, dispatchReceiver, DynamicDescriptorDiagnostic)
            }
        }

        return result
    }

    private fun ReceiverValueWithSmartCastInfo.smartCastReceiver(targetType: KotlinType): ReceiverValueWithSmartCastInfo {
        if (receiverValue !is ImplicitClassReceiver) return this

        val newReceiverValue = CastImplicitClassReceiver(receiverValue.classDescriptor, targetType)
        return ReceiverValueWithSmartCastInfo(newReceiverValue, possibleTypes, isStable)
    }

    override fun getVariables(name: Name, extensionReceiver: ReceiverValueWithSmartCastInfo?): Collection<CandidateWithBoundDispatchReceiver<VariableDescriptor>> {
        return collectMembers { getContributedVariables(name, location) }
    }

    override fun getObjects(name: Name, extensionReceiver: ReceiverValueWithSmartCastInfo?): Collection<CandidateWithBoundDispatchReceiver<VariableDescriptor>> {
        return emptyList()
    }

    override fun getFunctions(name: Name, extensionReceiver: ReceiverValueWithSmartCastInfo?): Collection<CandidateWithBoundDispatchReceiver<FunctionDescriptor>> {
        return collectMembers {
            getContributedFunctions(name, location) + it.getInnerConstructors(name, location) +
            syntheticScopes.collectSyntheticMemberFunctions(it.singletonOrEmptyList(), name, location)
        }
    }
}

internal class QualifierScopeTowerLevel(scopeTower: ImplicitScopeTower, val qualifier: QualifierReceiver) : AbstractScopeTowerLevel(scopeTower) {
    override fun getVariables(name: Name, extensionReceiver: ReceiverValueWithSmartCastInfo?) = qualifier.staticScope
            .getContributedVariables(name, location).map {
                createCandidateDescriptor(it, dispatchReceiver = null)
            }

    override fun getObjects(name: Name, extensionReceiver: ReceiverValueWithSmartCastInfo?) = qualifier.staticScope
            .getContributedObjectVariables(name, location).map {
                createCandidateDescriptor(it, dispatchReceiver = null)
            }

    override fun getFunctions(name: Name, extensionReceiver: ReceiverValueWithSmartCastInfo?) = qualifier.staticScope
            .getContributedFunctionsAndConstructors(name, location, scopeTower.syntheticConstructorsProvider).map {
                createCandidateDescriptor(it, dispatchReceiver = null)
            }
}

// KT-3335 Creating imported super class' inner class fails in codegen
internal open class ScopeBasedTowerLevel protected constructor(
        scopeTower: ImplicitScopeTower,
        private val resolutionScope: ResolutionScope
) : AbstractScopeTowerLevel(scopeTower) {

    internal constructor(scopeTower: ImplicitScopeTower, lexicalScope: LexicalScope) : this(scopeTower, lexicalScope as ResolutionScope)

    override fun getVariables(name: Name, extensionReceiver: ReceiverValueWithSmartCastInfo?): Collection<CandidateWithBoundDispatchReceiver<VariableDescriptor>>
            = resolutionScope.getContributedVariables(name, location).map {
                createCandidateDescriptor(it, dispatchReceiver = null)
            }

    override fun getObjects(name: Name, extensionReceiver: ReceiverValueWithSmartCastInfo?): Collection<CandidateWithBoundDispatchReceiver<VariableDescriptor>>
            = resolutionScope.getContributedObjectVariables(name, location).map {
                createCandidateDescriptor(it, dispatchReceiver = null)
            }

    override fun getFunctions(name: Name, extensionReceiver: ReceiverValueWithSmartCastInfo?): Collection<CandidateWithBoundDispatchReceiver<FunctionDescriptor>>
            = resolutionScope.getContributedFunctionsAndConstructors(name, location, scopeTower.syntheticConstructorsProvider).map {
                createCandidateDescriptor(it, dispatchReceiver = null)
            }
}
internal class ImportingScopeBasedTowerLevel(
        scopeTower: ImplicitScopeTower,
        importingScope: ImportingScope
): ScopeBasedTowerLevel(scopeTower, importingScope)

internal class SyntheticScopeBasedTowerLevel(
        scopeTower: ImplicitScopeTower,
        private val syntheticScopes: SyntheticScopes
): AbstractScopeTowerLevel(scopeTower) {
    private val ReceiverValueWithSmartCastInfo.allTypes: Set<KotlinType>
        get() = possibleTypes + receiverValue.type

    override fun getVariables(name: Name, extensionReceiver: ReceiverValueWithSmartCastInfo?): Collection<CandidateWithBoundDispatchReceiver<VariableDescriptor>> {
        if (extensionReceiver == null) return emptyList()

        return syntheticScopes.collectSyntheticExtensionProperties(extensionReceiver.allTypes, name, location).map {
            createCandidateDescriptor(it, dispatchReceiver = null)
        }
    }

    override fun getObjects(
            name: Name, extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<CandidateWithBoundDispatchReceiver<VariableDescriptor>> =
            emptyList()

    override fun getFunctions(
            name: Name,
            extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<CandidateWithBoundDispatchReceiver<FunctionDescriptor>> =
            emptyList()

}

internal class HidesMembersTowerLevel(scopeTower: ImplicitScopeTower): AbstractScopeTowerLevel(scopeTower) {
    override fun getVariables(name: Name, extensionReceiver: ReceiverValueWithSmartCastInfo?)
            = getCandidates(name, extensionReceiver, LexicalScope::collectVariables)

    override fun getObjects(name: Name, extensionReceiver: ReceiverValueWithSmartCastInfo?)
            = emptyList<CandidateWithBoundDispatchReceiver<VariableDescriptor>>()

    override fun getFunctions(name: Name, extensionReceiver: ReceiverValueWithSmartCastInfo?)
            = getCandidates(name, extensionReceiver, LexicalScope::collectFunctions)

    private fun <T: CallableDescriptor> getCandidates(
            name: Name,
            extensionReceiver: ReceiverValueWithSmartCastInfo?,
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

private fun ResolutionScope.getContributedFunctionsAndConstructors(
        name: Name,
        location: LookupLocation,
        syntheticConstructorsProvider: SyntheticConstructorsProvider
): Collection<FunctionDescriptor> {
    val result = ArrayList<FunctionDescriptor>(getContributedFunctions(name, location))

    val classifier = getContributedClassifier(name, location)
    if (classifier != null) {
        classifier.getCallableConstructors().filterTo(result) { it.dispatchReceiverParameter == null }
        syntheticConstructorsProvider.getSyntheticConstructors(classifier, location).filterTo(result) { it.dispatchReceiverParameter == null }
    }

    return result.toReadOnlyList()
}

private fun ClassifierDescriptor.getCallableConstructors(): Collection<FunctionDescriptor> =
        when (this) {
            is TypeAliasDescriptor ->
                getTypeAliasConstructors()
            is ClassDescriptor ->
                if (canHaveCallableConstructors) constructors else emptyList()
            else -> emptyList()
        }

private fun ResolutionScope.getContributedObjectVariables(name: Name, location: LookupLocation): Collection<VariableDescriptor> {
    val objectDescriptor = getFakeDescriptorForObject(getContributedClassifier(name, location))
    return listOfNotNull(objectDescriptor)
}

fun getFakeDescriptorForObject(classifier: ClassifierDescriptor?): FakeCallableDescriptorForObject? =
        when (classifier) {
            is TypeAliasDescriptor ->
                classifier.classDescriptor?.let { classDescriptor ->
                    if (classDescriptor.hasClassValueDescriptor)
                        FakeCallableDescriptorForTypeAliasObject(classifier)
                    else
                        null
                }
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

fun TypeAliasDescriptor.getTypeAliasConstructors(): Collection<TypeAliasConstructorDescriptor> {
    val classDescriptor = this.classDescriptor ?: return emptyList()
    if (!classDescriptor.canHaveCallableConstructors) return emptyList()

    val substitutor = this.getTypeSubstitutorForUnderlyingClass() ?:
                      throw AssertionError("classDescriptor should be non-null for $this")

    return classDescriptor.constructors.mapNotNull {
        TypeAliasConstructorDescriptorImpl.createIfAvailable(this, it, substitutor)
    }
}

private fun TypeAliasDescriptor.getTypeSubstitutorForUnderlyingClass(): TypeSubstitutor? {
    if (classDescriptor == null) return null

    val expandedTypeParameters = expandedType.constructor.parameters
    val expandedTypeArguments = expandedType.arguments
    return TypeSubstitutor.create(IndexedParametersSubstitution(expandedTypeParameters, expandedTypeArguments))
}
