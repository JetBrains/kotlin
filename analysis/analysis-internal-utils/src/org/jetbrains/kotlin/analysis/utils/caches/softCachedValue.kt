package org.jetbrains.kotlin.analysis.utils.caches

import com.intellij.openapi.project.Project
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import kotlin.reflect.KProperty


@Suppress("NOTHING_TO_INLINE")
public inline operator fun <T> CachedValue<T>.getValue(thisRef: Any?, property: KProperty<*>): T = value

public inline fun <T> softCachedValue(
    project: Project,
    vararg dependencies: Any,
    crossinline createValue: () -> T
): CachedValue<T> =
    CachedValuesManager.getManager(project).createCachedValue {
        CachedValueProvider.Result(
            createValue(),
            dependencies
        )
    }
