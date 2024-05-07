/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.elements

import com.intellij.psi.PsiTypeParameterListOwner
import com.intellij.psi.impl.light.LightTypeParameterBuilder
import org.jetbrains.kotlin.psi.KtTypeParameter

@Suppress("unused") // Used by UastFakeSourceLightMethod
open class KotlinLightTypeParameterBuilder(
    name: String,
    owner: PsiTypeParameterListOwner,
    index: Int,
    override val origin: KtTypeParameter
) : LightTypeParameterBuilder(name, owner, index), PsiElementWithOrigin<KtTypeParameter>
