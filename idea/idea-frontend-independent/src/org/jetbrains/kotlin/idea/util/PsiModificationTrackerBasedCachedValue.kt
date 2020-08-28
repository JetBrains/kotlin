/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.util

import com.intellij.openapi.project.Project
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Creates a value which will be cached until until any physical PSI change happens
 * To create one please use [psiModificationTrackerBasedCachedValue]
 *
 * @see com.intellij.psi.util.CachedValue
 * @see com.intellij.psi.util.PsiModificationTracker.MODIFICATION_COUNT
 */
class PsiModificationTrackerBasedCachedValue<T>(project: Project, createValue: () -> T) : ReadOnlyProperty<Any?, T> {
    private val cachedValue = CachedValuesManager.getManager(project).createCachedValue {
        CachedValueProvider.Result(
            createValue(),
            PsiModificationTracker.MODIFICATION_COUNT
        )
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T = cachedValue.value
}

/**
 * Creates a value which will be cached until until any physical PSI change happens
 *
 * @see com.intellij.psi.util.CachedValue
 * @see com.intellij.psi.util.PsiModificationTracker.MODIFICATION_COUNT
 * @see PsiModificationTrackerBasedCachedValue
 */
fun <T> psiModificationTrackerBasedCachedValue(project: Project, createValue: () -> T): PsiModificationTrackerBasedCachedValue<T> =
    PsiModificationTrackerBasedCachedValue(project, createValue)