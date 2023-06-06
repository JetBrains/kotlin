/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import org.jetbrains.kotlin.load.java.structure.LightClassOriginKind
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtScript

interface KtLightClassForScript : KtLightClass {
    val script: KtScript

    override val kotlinOrigin: KtClassOrObject?
        get() = null

    override val originKind: LightClassOriginKind
        get() = LightClassOriginKind.SOURCE
}