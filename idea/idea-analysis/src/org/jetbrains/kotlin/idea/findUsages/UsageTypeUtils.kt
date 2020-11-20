/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.findUsages

import com.intellij.psi.PsiElement
import com.intellij.usages.impl.rules.UsageTypeProviderEx
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance

//object UsageTypeUtils {
//    fun getUsageType(element: PsiElement?): UsageTypeEnum? =
//        UsageTypeProviderEx.EP_NAME.extensions.firstIsInstance<KotlinUsageTypeProvider>().getUsageTypeEnum(element)
//}