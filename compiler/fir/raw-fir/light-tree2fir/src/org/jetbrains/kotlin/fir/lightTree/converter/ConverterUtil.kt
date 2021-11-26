/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.converter

import com.intellij.lang.LighterASTNode
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtNodeTypes.*
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.builder.generateResolvedAccessExpression
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.declarations.builder.buildProperty
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.builder.FirCallBuilder
import org.jetbrains.kotlin.fir.expressions.builder.buildArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.buildBlock
import org.jetbrains.kotlin.fir.expressions.builder.buildComponentCall
import org.jetbrains.kotlin.fir.lightTree.fir.DestructuringDeclaration
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.FirUserTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.KtPsiUtil

fun String?.nameAsSafeName(defaultName: String = ""): Name {
    return when {
        this != null -> Name.identifier(KtPsiUtil.unquoteIdentifier(this))
        defaultName.isNotEmpty() -> Name.identifier(defaultName)
        else -> SpecialNames.NO_NAME_PROVIDED
    }
}

fun LighterASTNode.getAsStringWithoutBacktick(): String {
    return this.toString().replace("`", "")
}

fun <T : FirCallBuilder> T.extractArgumentsFrom(container: List<FirExpression>): T {
    argumentList = buildArgumentList {
        arguments += container
    }
    return this
}

inline fun isClassLocal(classNode: LighterASTNode, getParent: LighterASTNode.() -> LighterASTNode?): Boolean {
    var currentNode: LighterASTNode? = classNode
    while (currentNode != null) {
        val tokenType = currentNode.tokenType
        val parent = currentNode.getParent()
        val parentTokenType = parent?.tokenType
        if (tokenType == PROPERTY || tokenType == FUN) {
            val grandParent = parent?.getParent()
            when {
                parentTokenType == KT_FILE -> return true
                parentTokenType == CLASS_BODY && !(grandParent?.tokenType == OBJECT_DECLARATION && grandParent?.getParent()?.tokenType == OBJECT_LITERAL) -> return true
                parentTokenType == BLOCK && grandParent?.tokenType == SCRIPT -> return true
            }
        }
        // NB: enum entry nested classes are considered local by FIR design (see discussion in KT-45115)
        if (parentTokenType == ENUM_ENTRY) {
            return true
        }
        if (tokenType == BLOCK) {
            return true
        }
        currentNode = parent
    }
    return false
}

fun generateDestructuringBlock(
    moduleData: FirModuleData,
    multiDeclaration: DestructuringDeclaration,
    container: FirVariable,
    tmpVariable: Boolean
): FirBlock {
    return buildBlock {
        if (tmpVariable) {
            statements += container
        }
        val isVar = multiDeclaration.isVar
        for ((index, entry) in multiDeclaration.entries.withIndex()) {
            if (entry == null) continue
            statements += buildProperty {
                this.moduleData = moduleData
                origin = FirDeclarationOrigin.Source
                returnTypeRef = entry.returnTypeRef
                name = entry.name
                initializer = buildComponentCall {
                    val componentCallSource = entry.source?.fakeElement(KtFakeSourceElementKind.DesugaredComponentFunctionCall)
                    source = componentCallSource
                    explicitReceiver = generateResolvedAccessExpression(componentCallSource, container)
                    componentIndex = index + 1
                }
                this.isVar = isVar
                symbol = FirPropertySymbol(entry.name) // TODO?
                source = entry.source
                isLocal = true
                status = FirDeclarationStatusImpl(Visibilities.Local, Modality.FINAL)
                annotations += entry.annotations
            }
        }
    }
}

val FirUserTypeRef.isUnderscored get() = qualifier.lastOrNull()?.name?.asString() == "_"
