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

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintSystemError
import org.jetbrains.kotlin.resolve.calls.model.DiagnosticReporter
import org.jetbrains.kotlin.resolve.calls.model.KotlinCallArgument
import org.jetbrains.kotlin.resolve.calls.model.KotlinCallDiagnostic
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability.*
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.ResolutionScope
import org.jetbrains.kotlin.resolve.scopes.SyntheticScopes
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import org.jetbrains.kotlin.resolve.scopes.utils.parentsWithSelf
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeApproximator

interface ImplicitScopeTower {
    val lexicalScope: LexicalScope

    fun getImplicitReceiver(scope: LexicalScope): ReceiverValueWithSmartCastInfo?

    fun getContextReceivers(scope: LexicalScope): List<ReceiverValueWithSmartCastInfo>

    fun getNameForGivenImportAlias(name: Name): Name?

    val dynamicScope: MemberScope

    val syntheticScopes: SyntheticScopes

    val location: LookupLocation

    val isDebuggerContext: Boolean

    val isNewInferenceEnabled: Boolean

    val areContextReceiversEnabled: Boolean

    val languageVersionSettings: LanguageVersionSettings

    val typeApproximator: TypeApproximator

    val implicitsResolutionFilter: ImplicitsExtensionsResolutionFilter

    fun allScopesWithImplicitsResolutionInfo(): Sequence<ScopeWithImplicitsExtensionsResolutionInfo> =
        implicitsResolutionFilter.getScopesWithInfo(lexicalScope.parentsWithSelf)

    fun interceptFunctionCandidates(
        resolutionScope: ResolutionScope,
        name: Name,
        initialResults: Collection<FunctionDescriptor>,
        location: LookupLocation,
        dispatchReceiver: ReceiverValueWithSmartCastInfo?,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<FunctionDescriptor>

    fun interceptVariableCandidates(
        resolutionScope: ResolutionScope,
        name: Name,
        initialResults: Collection<VariableDescriptor>,
        location: LookupLocation,
        dispatchReceiver: ReceiverValueWithSmartCastInfo?,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<VariableDescriptor>
}

interface ScopeTowerLevel {
    fun getVariables(name: Name, extensionReceiver: ReceiverValueWithSmartCastInfo?): Collection<CandidateWithBoundDispatchReceiver>

    fun getObjects(name: Name, extensionReceiver: ReceiverValueWithSmartCastInfo?): Collection<CandidateWithBoundDispatchReceiver>

    fun getFunctions(name: Name, extensionReceiver: ReceiverValueWithSmartCastInfo?): Collection<CandidateWithBoundDispatchReceiver>

    fun recordLookup(name: Name)
}

class CandidateWithBoundDispatchReceiver(
    val dispatchReceiver: ReceiverValueWithSmartCastInfo?,
    val descriptor: CallableDescriptor,
    val diagnostics: List<ResolutionDiagnostic>
)

@JvmName("getResultApplicabilityForConstraintErrors")
fun getResultApplicability(diagnostics: Collection<ConstraintSystemError>): CandidateApplicability =
    diagnostics.minByOrNull { it.applicability }?.applicability ?: RESOLVED

@JvmName("getResultApplicabilityForCallDiagnostics")
fun getResultApplicability(diagnostics: Collection<KotlinCallDiagnostic>): CandidateApplicability =
    diagnostics.minByOrNull { it.candidateApplicability }?.candidateApplicability ?: RESOLVED

abstract class ResolutionDiagnostic(candidateApplicability: CandidateApplicability) :
    KotlinCallDiagnostic(candidateApplicability) {
    override fun report(reporter: DiagnosticReporter) {
        // do nothing
    }
}

class NoMatchingContextReceiver : ResolutionDiagnostic(INAPPLICABLE_WRONG_RECEIVER) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCall(this)
    }
}

class ContextReceiverAmbiguity : ResolutionDiagnostic(RESOLVED_WITH_ERROR) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCall(this)
    }
}

class UnsupportedContextualDeclarationCall : ResolutionDiagnostic(RESOLVED_WITH_ERROR) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCall(this)
    }
}

// todo error for this access from nested class
class VisibilityError(val invisibleMember: DeclarationDescriptorWithVisibility) : ResolutionDiagnostic(K1_RUNTIME_ERROR) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCall(this)
    }
}

class VisibilityErrorOnArgument(
    val argument: KotlinCallArgument,
    val invisibleMember: DeclarationDescriptorWithVisibility
) : ResolutionDiagnostic(K1_RUNTIME_ERROR) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCallArgument(argument, this)
    }
}

class NestedClassViaInstanceReference(val classDescriptor: ClassDescriptor) : ResolutionDiagnostic(K1_IMPOSSIBLE_TO_GENERATE)
class InnerClassViaStaticReference(val classDescriptor: ClassDescriptor) : ResolutionDiagnostic(K1_IMPOSSIBLE_TO_GENERATE)
class UnsupportedInnerClassCall(val message: String) : ResolutionDiagnostic(K1_IMPOSSIBLE_TO_GENERATE)
class UsedSmartCastForDispatchReceiver(val smartCastType: KotlinType) : ResolutionDiagnostic(RESOLVED)

object ErrorDescriptorDiagnostic : ResolutionDiagnostic(RESOLVED) // todo discuss and change to INAPPLICABLE
object LowPriorityDescriptorDiagnostic : ResolutionDiagnostic(RESOLVED_LOW_PRIORITY)
object DynamicDescriptorDiagnostic : ResolutionDiagnostic(RESOLVED_LOW_PRIORITY)
object UnstableSmartCastDiagnostic : ResolutionDiagnostic(UNSTABLE_SMARTCAST)
object HiddenExtensionRelatedToDynamicTypes : ResolutionDiagnostic(HIDDEN)
object HiddenDescriptor : ResolutionDiagnostic(HIDDEN)

object InvokeConventionCallNoOperatorModifier : ResolutionDiagnostic(CONVENTION_ERROR)
object InfixCallNoInfixModifier : ResolutionDiagnostic(CONVENTION_ERROR)
object DeprecatedUnaryPlusAsPlus : ResolutionDiagnostic(CONVENTION_ERROR)

class ResolvedUsingDeprecatedVisibility(val baseSourceScope: ResolutionScope, val lookupLocation: LookupLocation) : ResolutionDiagnostic(RESOLVED)
