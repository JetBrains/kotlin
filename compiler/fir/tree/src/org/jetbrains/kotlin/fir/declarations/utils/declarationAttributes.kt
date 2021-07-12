/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.utils

import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.name.Name

private object IsFromVarargKey : FirDeclarationDataKey()
private object IsReferredViaField : FirDeclarationDataKey()
private object IsFromPrimaryConstructor : FirDeclarationDataKey()
private object SourceElementKey : FirDeclarationDataKey()
private object ModuleNameKey : FirDeclarationDataKey()
private object DanglingTypeConstraintsKey : FirDeclarationDataKey()

var FirProperty.isFromVararg: Boolean? by FirDeclarationDataRegistry.data(IsFromVarargKey)
var FirProperty.isReferredViaField: Boolean? by FirDeclarationDataRegistry.data(IsReferredViaField)
var FirProperty.fromPrimaryConstructor: Boolean? by FirDeclarationDataRegistry.data(IsFromPrimaryConstructor)
var FirTypeAlias.sourceElement: SourceElement? by FirDeclarationDataRegistry.data(SourceElementKey)
var FirRegularClass.sourceElement: SourceElement? by FirDeclarationDataRegistry.data(SourceElementKey)
var FirRegularClass.moduleName: String? by FirDeclarationDataRegistry.data(ModuleNameKey)

/**
 * Constraint without corresponding type argument
 */
data class DanglingTypeConstraint(val name: Name, val source: FirSourceElement)

var <T> T.danglingTypeConstraints: List<DanglingTypeConstraint>?
        where T : FirDeclaration, T : FirTypeParameterRefsOwner
        by FirDeclarationDataRegistry.data(DanglingTypeConstraintsKey)

// ----------------------------------- Utils -----------------------------------

val FirMemberDeclaration.containerSource: SourceElement?
    get() = when (this) {
        is FirCallableDeclaration -> containerSource
        is FirRegularClass -> sourceElement
        is FirTypeAlias -> sourceElement
    }

// See [BindingContext.BACKING_FIELD_REQUIRED]
val FirProperty.hasBackingField: Boolean
    get() {
        if (isAbstract) return false
        if (delegate != null) return false
        when (origin) {
            FirDeclarationOrigin.SubstitutionOverride -> return false
            FirDeclarationOrigin.IntersectionOverride -> return false
            FirDeclarationOrigin.Delegated -> return false
            else -> {
                val getter = getter ?: return true
                if (isVar && setter == null) return true
                if (setter?.hasBody == false && setter?.isAbstract == false) return true
                if (!getter.hasBody && !getter.isAbstract) return true

                return isReferredViaField == true
            }
        }
    }

fun FirDeclaration.getDanglingTypeConstraintsOrEmpty(): List<DanglingTypeConstraint> {
    return when (this) {
        is FirRegularClass -> danglingTypeConstraints
        is FirSimpleFunction -> danglingTypeConstraints
        is FirProperty -> danglingTypeConstraints
        else -> null
    } ?: emptyList()
}
