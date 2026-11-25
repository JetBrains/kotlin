/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(KtImplementationDetail::class)

package org.jetbrains.kotlin.resolve

import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.syntax.SyntaxId
import org.jetbrains.kotlin.syntax.elementType
import org.jetbrains.kotlin.syntax.syntaxId

// PSI-based API preserved for compatibility with external clients (e.g., the IntelliJ Kotlin plugin).
// The source of truth is the parse-tree-agnostic `KotlinModifierHelpers`.

fun compatibility(first: KtKeywordToken, second: KtKeywordToken): Compatibility =
    KotlinModifierHelpers.compatibility(first.syntaxId(), second.syntaxId())

val featureDependencies: Map<KtModifierKeywordToken, List<LanguageFeature>> =
    KotlinModifierHelpers.featureDependencies.mapKeys { it.key.toModifierKeywordToken() }

val featureDependenciesTargets: Map<LanguageFeature, Set<KotlinTarget>> = KotlinModifierHelpers.featureDependenciesTargets

val defaultVisibilityTargetPredicate: TargetAllowedPredicate = KotlinModifierHelpers.defaultVisibilityTargetPredicate

val possibleTargetPredicateMap: Map<KtModifierKeywordToken, TargetAllowedPredicate> =
    KotlinModifierHelpers.possibleTargetPredicateMap.mapKeys { it.key.toModifierKeywordToken() }

// NOTE: deprecated targets must be possible!
val deprecatedTargetPredicateMap: Map<KtModifierKeywordToken, TargetAllowedPredicate> =
    KotlinModifierHelpers.deprecatedTargetPredicateMap.mapKeys { it.key.toModifierKeywordToken() }

val deprecatedParentTargetMap: Map<KtKeywordToken, Set<KotlinTarget>> =
    KotlinModifierHelpers.deprecatedParentTargetMap.mapKeys { it.key.toKeywordToken() }

// NOTE: redundant targets must be possible!
val redundantTargetMap: Map<KtKeywordToken, Set<KotlinTarget>> =
    KotlinModifierHelpers.redundantTargetMap.mapKeys { it.key.toKeywordToken() }

val allowedInContextParameters: Set<KtKeywordToken> =
    KotlinModifierHelpers.allowedInContextParameters.mapTo(mutableSetOf()) { it.toKeywordToken() }

val possibleParentTargetPredicateMap: Map<KtModifierKeywordToken, TargetAllowedPredicate> =
    KotlinModifierHelpers.possibleParentTargetPredicateMap.mapKeys { it.key.toModifierKeywordToken() }

private fun SyntaxId.toKeywordToken(): KtKeywordToken = toElementType() as KtKeywordToken

private fun SyntaxId.toModifierKeywordToken(): KtModifierKeywordToken = toElementType() as KtModifierKeywordToken

private fun SyntaxId.toElementType(): IElementType = elementType() ?: error("No element type for syntax ID $this")
