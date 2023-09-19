/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.utils

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassOwner
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

public val PsiClass.classIdIfNonLocal: ClassId?
    get() {
        val packageName = (containingFile as? PsiClassOwner)?.packageName ?: return null
        val qualifiedName = qualifiedName ?: return null
        val relatedClassName = qualifiedName.removePrefix("$packageName.")
        if (relatedClassName.isEmpty()) return null

        return ClassId(FqName(packageName), FqName(relatedClassName), isLocal = false)
    }
