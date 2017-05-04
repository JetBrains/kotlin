/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.SyntheticScopes
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import org.jetbrains.kotlin.types.KotlinType

interface ImplicitScopeTower {
    val lexicalScope: LexicalScope

    fun getImplicitReceiver(scope: LexicalScope): ReceiverValueWithSmartCastInfo?

    val dynamicScope: MemberScope

    val syntheticScopes: SyntheticScopes

    val location: LookupLocation

    val isDebuggerContext: Boolean
}

interface ScopeTowerLevel {
    fun getVariables(name: Name, extensionReceiver: ReceiverValueWithSmartCastInfo?): Collection<CandidateWithBoundDispatchReceiver>

    fun getObjects(name: Name, extensionReceiver: ReceiverValueWithSmartCastInfo?): Collection<CandidateWithBoundDispatchReceiver>

    fun getFunctions(name: Name, extensionReceiver: ReceiverValueWithSmartCastInfo?): Collection<CandidateWithBoundDispatchReceiver>
}

interface CandidateWithBoundDispatchReceiver {
    val descriptor: CallableDescriptor

    val diagnostics: List<ResolutionDiagnostic>

    val dispatchReceiver: ReceiverValueWithSmartCastInfo?

    fun copy(newDescriptor: CallableDescriptor): CandidateWithBoundDispatchReceiver
}

data class ResolutionCandidateStatus(val diagnostics: List<ResolutionDiagnostic>) {
    val resultingApplicability: ResolutionCandidateApplicability = diagnostics.asSequence().map { it.candidateLevel }.max()
                                                                   ?: ResolutionCandidateApplicability.RESOLVED
}

enum class ResolutionCandidateApplicability {
    RESOLVED, // call success or has uncompleted inference or in other words possible successful candidate
    RESOLVED_LOW_PRIORITY,
    CONVENTION_ERROR, // missing infix, operator etc
    MAY_THROW_RUNTIME_ERROR, // unsafe call or unstable smart cast
    RUNTIME_ERROR, // problems with visibility
    IMPOSSIBLE_TO_GENERATE, // access to outer class from nested
    INAPPLICABLE, // arguments not matched
    HIDDEN, // removed from resolve
    // todo wrong receiver
}

abstract class ResolutionDiagnostic(val candidateLevel: ResolutionCandidateApplicability)

// todo error for this access from nested class
class VisibilityError(val invisibleMember: DeclarationDescriptorWithVisibility): ResolutionDiagnostic(ResolutionCandidateApplicability.RUNTIME_ERROR)
class NestedClassViaInstanceReference(val classDescriptor: ClassDescriptor): ResolutionDiagnostic(ResolutionCandidateApplicability.IMPOSSIBLE_TO_GENERATE)
class InnerClassViaStaticReference(val classDescriptor: ClassDescriptor): ResolutionDiagnostic(ResolutionCandidateApplicability.IMPOSSIBLE_TO_GENERATE)
class UnsupportedInnerClassCall(val message: String): ResolutionDiagnostic(ResolutionCandidateApplicability.IMPOSSIBLE_TO_GENERATE)
class UsedSmartCastForDispatchReceiver(val smartCastType: KotlinType): ResolutionDiagnostic(ResolutionCandidateApplicability.RESOLVED)

object ErrorDescriptorDiagnostic : ResolutionDiagnostic(ResolutionCandidateApplicability.RESOLVED) // todo discuss and change to INAPPLICABLE
object LowPriorityDescriptorDiagnostic : ResolutionDiagnostic(ResolutionCandidateApplicability.RESOLVED_LOW_PRIORITY)
object DynamicDescriptorDiagnostic: ResolutionDiagnostic(ResolutionCandidateApplicability.RESOLVED_LOW_PRIORITY)
object UnstableSmartCastDiagnostic: ResolutionDiagnostic(ResolutionCandidateApplicability.MAY_THROW_RUNTIME_ERROR)
object HiddenExtensionRelatedToDynamicTypes : ResolutionDiagnostic(ResolutionCandidateApplicability.HIDDEN)
object HiddenDescriptor: ResolutionDiagnostic(ResolutionCandidateApplicability.HIDDEN)

object InvokeConventionCallNoOperatorModifier : ResolutionDiagnostic(ResolutionCandidateApplicability.CONVENTION_ERROR)
object InfixCallNoInfixModifier : ResolutionDiagnostic(ResolutionCandidateApplicability.CONVENTION_ERROR)
object DeprecatedUnaryPlusAsPlus : ResolutionDiagnostic(ResolutionCandidateApplicability.CONVENTION_ERROR)
