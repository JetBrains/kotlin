/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.psi.PsiClass
import com.intellij.psi.impl.source.PsiExtensibleClass
import org.jetbrains.kotlin.asJava.KtLightClassMarker
import org.jetbrains.kotlin.asJava.elements.KtLightDeclaration
import org.jetbrains.kotlin.psi.KtClassOrObject

interface KtLightClass : PsiClass, KtLightDeclaration<KtClassOrObject, PsiClass>, KtLightClassMarker

interface KtExtensibleLightClass : KtLightClass, PsiExtensibleClass