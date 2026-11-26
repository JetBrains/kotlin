/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.symbols.impl.FirLocalPropertySymbol

class FirAllModifierRenderer(private val staticPolicy: StaticPolicy) : FirModifierRenderer() {
    override fun renderModifiers(memberDeclaration: FirMemberDeclaration) {
        if (memberDeclaration !is FirProperty ||
            memberDeclaration.symbol !is FirLocalPropertySymbol && memberDeclaration.visibility != Visibilities.Local
        ) {
            renderModifier(memberDeclaration.visibility.asString())
            renderModifier(memberDeclaration.modalityAsString())
        }
        if (memberDeclaration.isExpect) {
            renderModifier("expect")
        }
        if (memberDeclaration.isActual) {
            renderModifier("actual")
        }
        if (memberDeclaration.isExternal) {
            renderModifier("external")
        }
        if (memberDeclaration is FirCallableDeclaration && memberDeclaration.isOverride) {
            renderModifier("override")
        }
        if (memberDeclaration is FirCallableDeclaration && memberDeclaration.isStatic) {
            renderModifier(staticPolicy.renderStatic(memberDeclaration))
        }
        if (memberDeclaration is FirClassLikeDeclaration && memberDeclaration.isInner) {
            renderModifier("inner")
        }

        // `companion/data/fun` modifiers are only valid for FirRegularClass, but we render them to make sure they are not
        // incorrectly loaded for other declarations during deserialization.
        if (memberDeclaration is FirClassLikeDeclaration && memberDeclaration.status.isCompanion) {
            renderModifier("companion")
        }
        if (memberDeclaration is FirClassLikeDeclaration && memberDeclaration.status.isData) {
            renderModifier("data")
        }
        // All Java interfaces are considered `fun` (functional interfaces) for resolution purposes
        // (see JavaSymbolProvider.createFirJavaClass). Don't render `fun` for Java interfaces; it's not a modifier in Java.
        val isJavaInterface =
            memberDeclaration is FirRegularClass && memberDeclaration.classKind == ClassKind.INTERFACE && memberDeclaration.isJava
        if (memberDeclaration is FirClassLikeDeclaration && memberDeclaration.status.isFun && !isJavaInterface) {
            renderModifier("fun")
        }

        if (memberDeclaration is FirClassLikeDeclaration && memberDeclaration.status.isRichError) {
            renderModifier("error")
        }

        if (memberDeclaration is FirCallableDeclaration && memberDeclaration.isInline || memberDeclaration is FirClass && memberDeclaration.isInline) {
            renderModifier("inline")
        }
        if (memberDeclaration is FirClass && memberDeclaration.isValue) {
            renderModifier("value")
        }
        if (memberDeclaration is FirCallableDeclaration && memberDeclaration.isOperator) {
            renderModifier("operator")
        }
        if (memberDeclaration is FirCallableDeclaration && memberDeclaration.isInfix) {
            renderModifier("infix")
        }
        if (memberDeclaration is FirCallableDeclaration && memberDeclaration.isTailRec) {
            renderModifier("tailrec")
        }
        if (memberDeclaration is FirCallableDeclaration && memberDeclaration.isSuspend) {
            renderModifier("suspend")
        }
        if (memberDeclaration is FirCallableDeclaration && memberDeclaration.isConst) {
            renderModifier("const")
        }
        if (memberDeclaration is FirCallableDeclaration && memberDeclaration.isLateInit) {
            renderModifier("lateinit")
        }
    }

    override fun renderModifiers(backingField: FirBackingField) {
        renderModifier(backingField.visibility.asString())
    }

    override fun renderModifiers(constructor: FirConstructor) {
        renderModifier(constructor.visibility.asString())
        if (constructor.isExpect) {
            renderModifier("expect")
        }
        if (constructor.isActual) {
            renderModifier("actual")
        }
    }

    override fun renderModifiers(propertyAccessor: FirPropertyAccessor) {
        renderModifier(propertyAccessor.visibility.asString())

        if (propertyAccessor.isStatic) {
            renderModifier(staticPolicy.renderStatic(propertyAccessor))
        }
        if (propertyAccessor.isInline) {
            renderModifier("inline")
        }
        if (propertyAccessor.isExternal) {
            renderModifier("external")
        }
    }

    override fun renderModifiers(anonymousFunction: FirAnonymousFunction) {
        if (anonymousFunction.isSuspend) {
            renderModifier("suspend")
        }
    }
}
