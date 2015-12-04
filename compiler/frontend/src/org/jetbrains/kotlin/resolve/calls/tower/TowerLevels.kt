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

package org.jetbrains.kotlin.resolve.calls.tower

import com.intellij.util.SmartList
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.util.FakeCallableDescriptorForObject
import org.jetbrains.kotlin.resolve.descriptorUtil.hasClassValueDescriptor
import org.jetbrains.kotlin.resolve.scopes.ImportingScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.ResolutionScope
import org.jetbrains.kotlin.resolve.scopes.receivers.QualifierReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isDynamic
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
            if (descriptor.isSynthesized) diagnostics.add(SynthesizedDescriptorDiagnostic)
            if (dispatchReceiverSmartCastType != null) diagnostics.add(UsedSmartCastForDispatchReceiver(dispatchReceiverSmartCastType))

            Visibilities.findInvisibleMember(
                    dispatchReceiver ?: ReceiverValue.NO_RECEIVER, descriptor,
                    scopeTower.lexicalScope.ownerDescriptor
            )?.let { diagnostics.add(VisibilityError(it)) }
        }
        return CandidateWithBoundDispatchReceiverImpl(dispatchReceiver, descriptor, diagnostics)
    }

}

// todo create error for constructors call and for implicit receiver
// todo KT-9538 Unresolved inner class via subclass reference
// todo Future plan: move constructors and fake variables for objects to class member scope.
internal abstract class ConstructorsAndFakeVariableHackLevelScope(scopeTower: ScopeTower) : AbstractScopeTowerLevel(scopeTower) {

    protected fun createConstructors(
            classifier: ClassifierDescriptor?,
            dispatchReceiver: ReceiverValue?,
            dispatchReceiverSmartCastType: KotlinType? = null,
            checkClass: (ClassDescriptor) -> ResolutionDiagnostic? = { null }
    ): Collection<CandidateWithBoundDispatchReceiver<FunctionDescriptor>> {
        val classDescriptor = getClassWithConstructors(classifier) ?: return emptyList()

        val specialError = checkClass(classDescriptor)
        return classDescriptor.constructors.map {

            val dispatchReceiverHack = if (dispatchReceiver == null && it.dispatchReceiverParameter != null) {
                it.dispatchReceiverParameter?.value // todo this is hack for Scope Level
            }
            else if (dispatchReceiver != null && it.dispatchReceiverParameter == null) { // should be reported error
                null
            }
            else {
                dispatchReceiver
            }

            createCandidateDescriptor(it, dispatchReceiverHack, specialError, dispatchReceiverSmartCastType)
        }
    }

    protected fun createVariableDescriptor(
            classifier: ClassifierDescriptor?,
            dispatchReceiverSmartCastType: KotlinType? = null,
            checkClass: (ClassDescriptor) -> ResolutionDiagnostic? = { null }
    ): CandidateWithBoundDispatchReceiver<VariableDescriptor>? {
        val fakeVariable = getFakeDescriptorForObject(classifier) ?: return null
        return createCandidateDescriptor(fakeVariable, null, checkClass(fakeVariable.classDescriptor), dispatchReceiverSmartCastType)
    }


    private fun getClassWithConstructors(classifier: ClassifierDescriptor?): ClassDescriptor? {
        if (classifier !is ClassDescriptor || ErrorUtils.isError(classifier)
            // Constructors of singletons shouldn't be callable from the code
            || classifier.kind.isSingleton) {
            return null
        }
        else {
            return classifier
        }
    }

    private fun getFakeDescriptorForObject(classifier: ClassifierDescriptor?): FakeCallableDescriptorForObject? {
        if (classifier !is ClassDescriptor || !classifier.hasClassValueDescriptor) return null // todo

        return FakeCallableDescriptorForObject(classifier)
    }
}

internal class ReceiverScopeTowerLevel(
        scopeTower: ScopeTower,
        val dispatchReceiver: ReceiverValue
): ConstructorsAndFakeVariableHackLevelScope(scopeTower) {
    private val memberScope = dispatchReceiver.type.memberScope

    private fun <D: CallableDescriptor> collectMembers(
            members: ResolutionScope.() -> Collection<D>,
            additionalDescriptors: ResolutionScope.(smartCastType: KotlinType?) -> Collection<CandidateWithBoundDispatchReceiver<D>> // todo
    ): Collection<CandidateWithBoundDispatchReceiver<D>> {
        val result = ArrayList<CandidateWithBoundDispatchReceiver<D>>(0)
        memberScope.members().mapTo(result) {
            createCandidateDescriptor(it, dispatchReceiver)
        }
        result.addAll(memberScope.additionalDescriptors(null))

        val smartCastPossibleTypes = scopeTower.dataFlowInfo.getSmartCastTypes(dispatchReceiver)
        val unstableError = if (scopeTower.dataFlowInfo.isStableReceiver(dispatchReceiver)) null else UnstableSmartCastDiagnostic

        for (possibleType in smartCastPossibleTypes) {
            possibleType.memberScope.members().mapTo(result) {
                createCandidateDescriptor(it, dispatchReceiver, unstableError, dispatchReceiverSmartCastType = possibleType)
            }

            possibleType.memberScope.additionalDescriptors(possibleType).mapTo(result) {
                it.addDiagnostic(unstableError)
            }
        }

        if (dispatchReceiver.type.isDynamic()) {
            scopeTower.dynamicScope.members().mapTo(result) {
                createCandidateDescriptor(it, dispatchReceiver, DynamicDescriptorDiagnostic)
            }
        }

        return result
    }

    // todo add static methods & fields with error
    override fun getVariables(name: Name): Collection<CandidateWithBoundDispatchReceiver<VariableDescriptor>> {
        return collectMembers({ getContributedVariables(name, location) }) {
            smartCastType ->
            listOfNotNull(createVariableDescriptor(getContributedClassifier(name, location), smartCastType){ NestedClassViaInstanceReference(it) } )
        }
    }

    override fun getFunctions(name: Name): Collection<CandidateWithBoundDispatchReceiver<FunctionDescriptor>> {
        return collectMembers({ getContributedFunctions(name, location) }) {
            smartCastType ->
            createConstructors(getContributedClassifier(name, location), dispatchReceiver, smartCastType) {
                if (it.isInner) null else NestedClassViaInstanceReference(it)
            }
        }
    }
}

internal class QualifierScopeTowerLevel(scopeTower: ScopeTower, qualifier: QualifierReceiver) : ConstructorsAndFakeVariableHackLevelScope(scopeTower) {
    private val qualifierScope = qualifier.getNestedClassesAndPackageMembersScope()

    override fun getVariables(name: Name): Collection<CandidateWithBoundDispatchReceiver<VariableDescriptor>> {
        val result = ArrayList<CandidateWithBoundDispatchReceiver<VariableDescriptor>>(0)
        qualifierScope.getContributedVariables(name, location).mapTo(result) {
            createCandidateDescriptor(it, dispatchReceiver = null)
        }
        result.addIfNotNull(createVariableDescriptor(qualifierScope.getContributedClassifier(name, location)))
        return result
    }

    override fun getFunctions(name: Name): Collection<CandidateWithBoundDispatchReceiver<FunctionDescriptor>> {
        val functions = qualifierScope.getContributedFunctions(name, location).map {
            createCandidateDescriptor(it, dispatchReceiver = null)
        }
        val constructors = createConstructors(qualifierScope.getContributedClassifier(name, location), dispatchReceiver = null) {
            if (it.isInner) InnerClassViaStaticReference(it) else null
        }
        return functions + constructors
    }
}

internal open class ScopeBasedTowerLevel protected constructor(
        scopeTower: ScopeTower,
        private val resolutionScope: ResolutionScope
) : ConstructorsAndFakeVariableHackLevelScope(scopeTower) {

    internal constructor(scopeTower: ScopeTower, lexicalScope: LexicalScope): this(scopeTower, lexicalScope as ResolutionScope)

    override fun getVariables(name: Name): Collection<CandidateWithBoundDispatchReceiver<VariableDescriptor>> {
        val result = ArrayList<CandidateWithBoundDispatchReceiver<VariableDescriptor>>(0)
        resolutionScope.getContributedVariables(name, location).mapTo(result) {
            createCandidateDescriptor(it, dispatchReceiver = null)
        }
        result.addIfNotNull(createVariableDescriptor(resolutionScope.getContributedClassifier(name, location)))
        return result
    }

    override fun getFunctions(name: Name): Collection<CandidateWithBoundDispatchReceiver<FunctionDescriptor>> {
        val result = ArrayList<CandidateWithBoundDispatchReceiver<FunctionDescriptor>>(0)
        resolutionScope.getContributedFunctions(name, location).mapTo(result) {
            createCandidateDescriptor(it, dispatchReceiver = null)
        }

        // todo report errors for constructors if there is no match receiver
        result.addAll(createConstructors(resolutionScope.getContributedClassifier(name, location), dispatchReceiver = null) {
            if (!it.isInner) return@createConstructors null

            // todo add constructors functions to member class member scope
            // KT-3335 Creating imported super class' inner class fails in codegen
            UnsupportedInnerClassCall("Constructor call for inner class from subclass unsupported")
        })
        return result
    }
}

internal class ImportingScopeBasedTowerLevel(
        scopeTower: ScopeTower,
        private val importingScope: ImportingScope,
        private val receiversForSyntheticExtensions: Collection<KotlinType>
): ScopeBasedTowerLevel(scopeTower, importingScope) {

    override fun getVariables(name: Name): Collection<CandidateWithBoundDispatchReceiver<VariableDescriptor>> {
        val synthetic = importingScope.getContributedSyntheticExtensionProperties(receiversForSyntheticExtensions, name, location).map {
            createCandidateDescriptor(it, dispatchReceiver = null)
        }
        return super.getVariables(name) + synthetic
    }

    override fun getFunctions(name: Name): Collection<CandidateWithBoundDispatchReceiver<FunctionDescriptor>> {
        val synthetic = importingScope.getContributedSyntheticExtensionFunctions(receiversForSyntheticExtensions, name, location).map {
            createCandidateDescriptor(it, dispatchReceiver = null)
        }
        return super.getFunctions(name) + synthetic
    }
}