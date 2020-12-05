/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability

abstract class ResolutionDiagnostic(val applicability: CandidateApplicability)

abstract class InapplicableArgumentDiagnostic : ResolutionDiagnostic(CandidateApplicability.INAPPLICABLE) {
    abstract val argument: FirExpression
}

class MixingNamedAndPositionArguments(override val argument: FirExpression) : InapplicableArgumentDiagnostic()

class TooManyArguments(
    val argument: FirExpression,
    val function: FirFunction<*>
) : ResolutionDiagnostic(CandidateApplicability.INAPPLICABLE_ARGUMENTS_MAPPING_ERROR)

class NamedArgumentNotAllowed(
    override val argument: FirExpression,
    val function: FirFunction<*>
) : InapplicableArgumentDiagnostic()

class ArgumentPassedTwice(
    override val argument: FirExpression,
    val valueParameter: FirValueParameter,
    val firstOccurrence: ResolvedCallArgument
) : InapplicableArgumentDiagnostic()

class VarargArgumentOutsideParentheses(
    override val argument: FirExpression,
    val valueParameter: FirValueParameter
) : InapplicableArgumentDiagnostic()

class NonVarargSpread(override val argument: FirExpression) : InapplicableArgumentDiagnostic()

class NoValueForParameter(
    val valueParameter: FirValueParameter,
    val function: FirFunction<*>
) : ResolutionDiagnostic(CandidateApplicability.INAPPLICABLE_ARGUMENTS_MAPPING_ERROR)

class NameNotFound(
    override val argument: FirExpression,
    val function: FirFunction<*>
) : InapplicableArgumentDiagnostic()

object InapplicableCandidate : ResolutionDiagnostic(CandidateApplicability.INAPPLICABLE)

object HiddenCandidate : ResolutionDiagnostic(CandidateApplicability.HIDDEN)

object ResolvedWithLowPriority : ResolutionDiagnostic(CandidateApplicability.RESOLVED_LOW_PRIORITY)

object InapplicableWrongReceiver : ResolutionDiagnostic(CandidateApplicability.INAPPLICABLE_WRONG_RECEIVER)

object LowerPriorityToPreserveCompatibilityDiagnostic : ResolutionDiagnostic(CandidateApplicability.RESOLVED_NEED_PRESERVE_COMPATIBILITY)

object CandidateChosenUsingOverloadResolutionByLambdaAnnotation : ResolutionDiagnostic(CandidateApplicability.RESOLVED)
