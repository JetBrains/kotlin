/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.diagnostics.rendering

import org.jetbrains.kotlin.config.LanguageFeature

// This file was generated automatically. See FeatureToFlagMapGenerator.kt
// Please declare arguments in compiler/arguments/src/org/jetbrains/kotlin/arguments/description/CommonCompilerArguments.kt
// DO NOT MODIFY IT MANUALLY.

val featureToEnablingFlagMap: Map<LanguageFeature, String> = mapOf(
    LanguageFeature.AllowCheckForErasedTypesInContracts to "-Xallow-contracts-on-more-functions",
    LanguageFeature.AllowContractsOnPropertyAccessors to "-Xallow-contracts-on-more-functions",
    LanguageFeature.AllowContractsOnSomeOperators to "-Xallow-contracts-on-more-functions",
    LanguageFeature.AllowReifiedTypeInCatchClause to "-Xallow-reified-type-in-catch",
    LanguageFeature.AllowReturnsResultOfContract to "-Xallow-returns-result-of",
    LanguageFeature.AnnotationAllUseSiteTarget to "-Xannotation-target-all",
    LanguageFeature.AnnotationDefaultTargetMigrationWarning to "-Xannotation-default-target=first-only-warn",
    LanguageFeature.BreakContinueInInlineLambdas to "-Xnon-local-break-continue",
    LanguageFeature.CallCompletionRefinementsFor25 to "-Xeager-lambda-analysis",
    LanguageFeature.CallableReferencesToContextual to "-Xcallable-references-to-contextual",
    LanguageFeature.CollectionLiterals to "-Xcollection-literals",
    LanguageFeature.CompanionBlocks to "-Xcompanion-blocks",
    LanguageFeature.CompanionExtensions to "-Xcompanion-blocks-and-extensions",
    LanguageFeature.ConditionImpliesReturnsContracts to "-Xallow-condition-implies-returns-contracts",
    LanguageFeature.ContextParameters to "-Xcontext-parameters",
    LanguageFeature.ContextSensitiveResolutionUsingExpectedType to "-Xcontext-sensitive-resolution",
    LanguageFeature.DataClassCopyRespectsConstructorVisibility to "-Xconsistent-data-class-copy-visibility",
    LanguageFeature.DataFlowBasedExhaustiveness to "-Xdata-flow-based-exhaustiveness",
    LanguageFeature.DeprecateNameMismatchInShortDestructuringWithParentheses to "-Xname-based-destructuring={name-mismatch|complete}",
    LanguageFeature.DirectJavaActualization to "-Xdirect-java-actualization",
    LanguageFeature.DisableCompatibilityModeForNewInference to "-Xnew-inference",
    LanguageFeature.EagerLambdaAnalysis to "-Xeager-lambda-analysis",
    LanguageFeature.EnableNameBasedDestructuringShortForm to "-Xname-based-destructuring=complete",
    LanguageFeature.ExplicitBackingFields to "-Xexplicit-backing-fields",
    LanguageFeature.ExplicitContextArguments to "-Xexplicit-context-arguments",
    LanguageFeature.FunctionReferenceWithDefaultValueAsOtherType to "-Xnew-inference",
    LanguageFeature.HoldsInContracts to "-Xallow-holdsin-contract",
    LanguageFeature.InferThrowableTypeParameterToUpperBound to "-Xeager-lambda-analysis",
    LanguageFeature.InlineClasses to "-Xinline-classes",
    LanguageFeature.IntrinsicConstEvaluation to "-Xintrinsic-const-evaluation",
    LanguageFeature.LocalTypeAliases to "-Xlocal-type-aliases",
    LanguageFeature.MultiDollarInterpolation to "-Xmulti-dollar-interpolation",
    LanguageFeature.MultiPlatformProjects to "-Xmulti-platform",
    LanguageFeature.NameBasedDestructuring to "-Xname-based-destructuring={only-syntax|name-mismatch|complete}",
    LanguageFeature.NestedTypeAliases to "-Xnested-type-aliases",
    LanguageFeature.NewInference to "-Xnew-inference",
    LanguageFeature.PropertyParamAnnotationDefaultTargetMode to "-Xannotation-default-target=param-property",
    LanguageFeature.SamConversionPerArgument to "-Xnew-inference",
    LanguageFeature.UnrestrictedBuilderInference to "-Xunrestricted-builder-inference",
    LanguageFeature.WhenGuards to "-Xwhen-guards",
)
