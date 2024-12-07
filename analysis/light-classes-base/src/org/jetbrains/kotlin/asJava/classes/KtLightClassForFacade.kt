/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile

interface KtLightClassForFacade : KtLightClass {
    val facadeClassFqName: FqName
    val files: Collection<KtFile>
    val multiFileClass: Boolean

    override fun getName(): String = facadeClassFqName.shortName().asString()
}
