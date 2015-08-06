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

package org.jetbrains.kotlin.resolve

import com.intellij.lang.ASTNode
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.JetModifierKeywordToken
import java.util.*
import org.jetbrains.kotlin.lexer.JetTokens.*
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget.*
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*

public object ModifierCheckerCore {
    private enum class Compatibility {
        COMPATIBLE,
        REDUNDANT,
        REVERSE_REDUNDANT,
        REPEATED,
        INCOMPATIBLE
    }

    private val possibleTargetMap = mapOf<JetModifierKeywordToken, Set<KotlinTarget>>(
            ENUM_KEYWORD      to EnumSet.of(ENUM_CLASS),
            ABSTRACT_KEYWORD  to EnumSet.of(CLASS, LOCAL_CLASS, INNER_CLASS, INTERFACE, MEMBER_PROPERTY, MEMBER_FUNCTION),
            OPEN_KEYWORD      to EnumSet.of(CLASS, LOCAL_CLASS, INNER_CLASS, INTERFACE, MEMBER_PROPERTY, MEMBER_FUNCTION),
            FINAL_KEYWORD     to EnumSet.of(CLASS, LOCAL_CLASS, INNER_CLASS, ENUM_CLASS, OBJECT, MEMBER_PROPERTY, MEMBER_FUNCTION),
            SEALED_KEYWORD    to EnumSet.of(CLASS, LOCAL_CLASS, INNER_CLASS),
            INNER_KEYWORD     to EnumSet.of(INNER_CLASS),
            OVERRIDE_KEYWORD  to EnumSet.of(MEMBER_PROPERTY, MEMBER_FUNCTION),
            PRIVATE_KEYWORD   to EnumSet.of(CLASSIFIER, MEMBER_FUNCTION, TOP_LEVEL_FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER,
                                            MEMBER_PROPERTY, TOP_LEVEL_PROPERTY, CONSTRUCTOR),
            PUBLIC_KEYWORD    to EnumSet.of(CLASSIFIER, MEMBER_FUNCTION, TOP_LEVEL_FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER,
                                            MEMBER_PROPERTY, TOP_LEVEL_PROPERTY, CONSTRUCTOR),
            INTERNAL_KEYWORD  to EnumSet.of(CLASSIFIER, MEMBER_FUNCTION, TOP_LEVEL_FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER,
                                            MEMBER_PROPERTY, TOP_LEVEL_PROPERTY, CONSTRUCTOR),
            PROTECTED_KEYWORD to EnumSet.of(CLASSIFIER, MEMBER_FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, MEMBER_PROPERTY, CONSTRUCTOR),
            IN_KEYWORD        to EnumSet.of(TYPE_PARAMETER, TYPE_PROJECTION),
            OUT_KEYWORD       to EnumSet.of(TYPE_PARAMETER, TYPE_PROJECTION),
            REIFIED_KEYWORD   to EnumSet.of(TYPE_PARAMETER),
            VARARG_KEYWORD    to EnumSet.of(VALUE_PARAMETER, PROPERTY_PARAMETER),
            COMPANION_KEYWORD to EnumSet.of(OBJECT)
    )

    // NOTE: redundant targets must be possible!
    private val redundantTargetMap = mapOf<JetModifierKeywordToken, Set<KotlinTarget>>(
            ABSTRACT_KEYWORD  to EnumSet.of(INTERFACE),
            OPEN_KEYWORD      to EnumSet.of(INTERFACE),
            FINAL_KEYWORD     to EnumSet.of(ENUM_CLASS, OBJECT)
    )

    private val possibleParentTargetMap = mapOf<JetModifierKeywordToken, Set<KotlinTarget>>(
            INNER_KEYWORD     to EnumSet.of(CLASS, INNER_CLASS, LOCAL_CLASS, ENUM_CLASS, ENUM_ENTRY),
            OVERRIDE_KEYWORD  to EnumSet.of(CLASSIFIER, ENUM_ENTRY),
            PROTECTED_KEYWORD to EnumSet.of(CLASSIFIER, ENUM_ENTRY),
            COMPANION_KEYWORD to EnumSet.of(CLASS, ENUM_CLASS, INTERFACE)
    )

    // First modifier in pair should be also first in declaration
    private val mutualCompatibility = buildCompatibilityMap()

    private fun buildCompatibilityMap(): Map<Pair<JetModifierKeywordToken, JetModifierKeywordToken>, Compatibility> {
        val result = hashMapOf<Pair<JetModifierKeywordToken, JetModifierKeywordToken>, Compatibility>()
        // Variance: in + out are incompatible
        result += incompatibilityRegister(listOf(IN_KEYWORD, OUT_KEYWORD))
        // Visibilities: incompatible
        result += incompatibilityRegister(listOf(PRIVATE_KEYWORD, PROTECTED_KEYWORD, PUBLIC_KEYWORD, INTERNAL_KEYWORD))
        // Abstract + open + final + sealed: incompatible
        result += incompatibilityRegister(listOf(ABSTRACT_KEYWORD, OPEN_KEYWORD, FINAL_KEYWORD, SEALED_KEYWORD))
        // open is redundant to abstract & override
        result += redundantRegister(ABSTRACT_KEYWORD, OPEN_KEYWORD)
        result += redundantRegister(OVERRIDE_KEYWORD, OPEN_KEYWORD)
        // abstract is redundant to sealed
        result += redundantRegister(SEALED_KEYWORD, ABSTRACT_KEYWORD)
        return result
    }

    private fun redundantRegister(
            sufficient: JetModifierKeywordToken,
            redundant: JetModifierKeywordToken
    ): Map<Pair<JetModifierKeywordToken, JetModifierKeywordToken>, Compatibility> {
        return mapOf(Pair(sufficient, redundant) to Compatibility.REDUNDANT,
                     Pair(redundant, sufficient) to Compatibility.REVERSE_REDUNDANT)
    }

    private fun incompatibilityRegister(
            list: List<JetModifierKeywordToken>
    ): Map<Pair<JetModifierKeywordToken, JetModifierKeywordToken>, Compatibility> {
        val result = hashMapOf<Pair<JetModifierKeywordToken, JetModifierKeywordToken>, Compatibility>()
        for (first in list) {
            for (second in list) {
                if (first != second) {
                    result[Pair(first, second)] = Compatibility.INCOMPATIBLE
                }
            }
        }
        return result
    }

    private fun compatibility(first: JetModifierKeywordToken, second: JetModifierKeywordToken): Compatibility {
        if (first == second) {
            return Compatibility.REPEATED
        }
        else {
            return mutualCompatibility[Pair(first, second)] ?: Compatibility.COMPATIBLE
        }
    }

    private fun checkCompatibility(trace: BindingTrace, firstNode: ASTNode, secondNode: ASTNode, incorrectNodes: MutableSet<ASTNode>) {
        val first = firstNode.elementType as JetModifierKeywordToken
        val second = secondNode.elementType as JetModifierKeywordToken
        val compatibility = compatibility(first, second)
        when (compatibility) {
            Compatibility.COMPATIBLE -> {}
            Compatibility.REPEATED -> if (incorrectNodes.add(secondNode)) {
                trace.report(Errors.REPEATED_MODIFIER.on (secondNode.psi, first))
            }
            Compatibility.REDUNDANT -> if (incorrectNodes.add(secondNode)) {
                trace.report(Errors.REDUNDANT_MODIFIER.on(secondNode.psi, first, second))
            }
            Compatibility.REVERSE_REDUNDANT -> if (incorrectNodes.add(firstNode)) {
                trace.report(Errors.REDUNDANT_MODIFIER.on(firstNode.psi,  second, first))
            }
            Compatibility.INCOMPATIBLE -> {
                if (incorrectNodes.add(firstNode)) {
                    trace.report(Errors.INCOMPATIBLE_MODIFIERS.on(firstNode.psi, first, second))
                }
                if (incorrectNodes.add(secondNode)) {
                    trace.report(Errors.INCOMPATIBLE_MODIFIERS.on(secondNode.psi, second, first))
                }
            }
        }
    }

    private fun checkTarget(trace: BindingTrace, node: ASTNode, actualTargets: List<KotlinTarget>): Boolean {
        val modifier = node.elementType as JetModifierKeywordToken
        val possibleTargets = possibleTargetMap[modifier] ?: emptySet()
        if (!actualTargets.any { it in possibleTargets }) {
            trace.report(Errors.WRONG_MODIFIER_TARGET.on(node.psi, modifier, actualTargets.firstOrNull()?.description ?: "this"))
            return false
        }
        val redundantTargets = redundantTargetMap[modifier] ?: emptySet()
        if (actualTargets.any { it in redundantTargets}) {
            trace.report(Errors.REDUNDANT_MODIFIER_FOR_TARGET.on(node.psi, modifier, actualTargets.firstOrNull()?.description ?: "this"))
        }
        return true
    }

    private fun checkParent(trace: BindingTrace, node: ASTNode, parentDescriptor: DeclarationDescriptor?): Boolean {
        val modifier = node.elementType as JetModifierKeywordToken
        val actualParents: List<KotlinTarget> = when (parentDescriptor) {
            is ClassDescriptor -> KotlinTarget.classActualTargets(parentDescriptor)
            is FunctionDescriptor -> listOf(FUNCTION)
            else -> listOf(PACKAGE)
        }
        val possibleParents = possibleParentTargetMap[modifier] ?: return true
        if (possibleParents == KotlinTarget.ALL_TARGET_SET) return true
        if (actualParents.any { it in possibleParents }) return true
        trace.report(Errors.WRONG_MODIFIER_CONTAINING_DECLARATION.on(node.psi, modifier, actualParents.firstOrNull()?.description ?: "this scope"))
        return false
    }

    private val MODIFIER_KEYWORD_SET = TokenSet.orSet(JetTokens.SOFT_KEYWORDS, TokenSet.create(JetTokens.IN_KEYWORD))

    private fun checkModifierList(list: JetModifierList, trace: BindingTrace, parentDescriptor: DeclarationDescriptor?, actualTargets: List<KotlinTarget>) {
        val incorrectNodes = hashSetOf<ASTNode>()
        val children = list.node.getChildren(MODIFIER_KEYWORD_SET)
        for (second in children) {
            for (first in children) {
                if (first == second) {
                    break;
                }
                checkCompatibility(trace, first, second, incorrectNodes)
            }
            if (second !in incorrectNodes) {
                if (!checkTarget(trace, second, actualTargets)) {
                    incorrectNodes += second
                }
                else if (!checkParent(trace, second, parentDescriptor)) {
                    incorrectNodes += second
                }
            }
        }
    }

    public fun check(listOwner: JetModifierListOwner, trace: BindingTrace, descriptor: DeclarationDescriptor?) {
        if (listOwner is JetFunction) {
            for (parameter in listOwner.valueParameters) {
                if (!parameter.hasValOrVar()) {
                    check(parameter, trace, null)
                }
            }
        }
        val actualTargets = AnnotationChecker.getActualTargetList(listOwner, descriptor as? ClassDescriptor)
        val list = listOwner.modifierList ?: return
        checkModifierList(list, trace, descriptor?.containingDeclaration, actualTargets)
    }
}