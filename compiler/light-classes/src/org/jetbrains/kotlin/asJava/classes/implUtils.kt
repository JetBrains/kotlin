/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtSuperTypeList
import org.jetbrains.kotlin.psi.KtSuperTypeListEntry
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe

fun KtSuperTypeList.findEntry(fqNameToFind: String): KtSuperTypeListEntry? {
    val context = LightClassGenerationSupport.getInstance(project).analyzeWithContent(parent as KtClassOrObject)
    return entries.firstOrNull {
        val referencedType = context[BindingContext.TYPE, it.typeReference]
        referencedType?.constructor?.declarationDescriptor?.fqNameUnsafe?.asString() == fqNameToFind
    }
}
