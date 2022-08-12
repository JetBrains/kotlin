/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightFieldForObject
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightAccessorMethod

val PsiField.isFieldForObjectInstance: Boolean
    get() = this is SymbolLightFieldForObject

val PsiMethod.isPropertyAccessor: Boolean
    get() = this is SymbolLightAccessorMethod

val PsiMethod.isPropertyGetter: Boolean
    get() = (this as? SymbolLightAccessorMethod)?.isGetter == true

val PsiMethod.isPropertySetter: Boolean
    get() = (this as? SymbolLightAccessorMethod)?.isGetter == false

context(KtAnalysisSession)
val PsiMethod.correspondingPropertyHasBackingField: Boolean
    get() = (this as? SymbolLightAccessorMethod)?.containingPropertySymbolPointer?.restoreSymbol()?.hasBackingField == true
