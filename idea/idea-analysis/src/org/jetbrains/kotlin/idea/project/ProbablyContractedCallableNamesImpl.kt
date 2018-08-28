/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.project

import com.intellij.openapi.project.Project
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import org.jetbrains.kotlin.idea.stubindex.KotlinProbablyContractedFunctionShortNameIndex
import org.jetbrains.kotlin.resolve.lazy.ProbablyContractedCallableNames

class ProbablyContractedCallableNamesImpl(project: Project) : ProbablyContractedCallableNames {
    private val functionNames = CachedValuesManager.getManager(project).createCachedValue(
        {
            CachedValueProvider.Result.create(
                KotlinProbablyContractedFunctionShortNameIndex.getInstance().getAllKeys(project),
                PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT
            )
        },
        false
    )

    override fun isProbablyContractedCallableName(name: String): Boolean = name in functionNames.value
}