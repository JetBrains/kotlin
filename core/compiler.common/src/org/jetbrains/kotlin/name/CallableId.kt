/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.name

/**
 * A callable ID identifies a Kotlin callable, such as a function or property. When [className] is `null`, the ID represents a top-level
 * callable.
 *
 * In case of overloads, multiple callables may share the same [CallableId].
 *
 * #### Example
 *
 * ```kotlin
 * package one.two
 *
 * fun foo() {
 *     fun loc() { }
 * }
 *
 * class A {
 *     val bar: String = "bar"
 *
 *     class B {
 *          fun nes() { }
 *     }
 * }
 *
 * fun overloaded(value: Int) { }
 * fun overloaded(value: String) { }
 * ```
 *
 * Callable IDs for the callables above:
 *
 * - `foo`: `one/two/foo`
 * - `loc`: `<local>/loc` (see [SpecialNames.LOCAL])
 * - `bar`: `one/two/A.bar`
 * - `nes`: `one/two/A.B.nes`
 * - `overloaded`: `one/two/overloaded` for *both* `overloaded(value: Int)` and `overloaded(value: String)`
 */
class CallableId private constructor(
    val packageName: FqName,
    val className: FqName?,
    val callableName: Name,
    val classId: ClassId?,
    // Currently, it's only used for debug info
    private val pathToLocal: FqName?
) {
    companion object {
        private val LOCAL_NAME = SpecialNames.LOCAL
        val PACKAGE_FQ_NAME_FOR_LOCAL = FqName.topLevel(LOCAL_NAME)

        private fun calculateClassId(packageName: FqName, className: FqName?): ClassId? =
            className?.let { ClassId(packageName, it, isLocal = packageName == PACKAGE_FQ_NAME_FOR_LOCAL) }
    }

    /**
     * Returns `true` if the corresponding callable declaration is itself local, or if it is a member of a local class.
     * Otherwise, returns `false`.
     */
    val isLocal: Boolean
        get() = packageName == PACKAGE_FQ_NAME_FOR_LOCAL || classId?.isLocal == true

    constructor(
        packageName: FqName, className: FqName?, callableName: Name,
    ) : this(packageName, className, callableName, calculateClassId(packageName, className), pathToLocal = null)

    constructor(
        packageName: FqName, className: FqName?, callableName: Name,
        // Currently, it's only used for debug info
        pathToLocal: FqName?
    ) : this(packageName, className, callableName, calculateClassId(packageName, className), pathToLocal)

    constructor(classId: ClassId, callableName: Name) :
            this(classId.packageFqName, classId.relativeClassName, callableName, classId, pathToLocal = null)

    constructor(packageName: FqName, callableName: Name) :
            this(packageName, className = null, callableName, classId = null, pathToLocal = null)

    constructor(
        callableName: Name,
        // Currently, it's only used for debug info
        pathToLocal: FqName?
    ) : this(PACKAGE_FQ_NAME_FOR_LOCAL, className = null, callableName, classId = null, pathToLocal)

    constructor(callableName: Name) : this(PACKAGE_FQ_NAME_FOR_LOCAL, className = null, callableName, classId = null, pathToLocal = null)

    fun asFqNameForDebugInfo(): FqName {
        pathToLocal?.child(callableName)?.let { return it }
        return asSingleFqName()
    }

    fun asSingleFqName(): FqName {
        return classId?.asSingleFqName()?.child(callableName) ?: packageName.child(callableName)
    }

    fun copy(callableName: Name): CallableId = CallableId(packageName, className, callableName, classId, pathToLocal)

    override fun equals(other: Any?): Boolean {
        return when {
            this === other -> true
            other !is CallableId -> false
            // classId isn't needed
            else -> packageName == other.packageName && className == other.className && callableName == other.callableName
        }
    }

    override fun hashCode(): Int {
        var result = 17
        result = result * 31 + packageName.hashCode()
        result = result * 31 + className.hashCode()
        result = result * 31 + callableName.hashCode()
        return result
    }

    override fun toString(): String {
        return buildString {
            append(packageName.asString().replace('.', '/'))
            append("/")
            if (className != null) {
                append(className)
                append(".")
            }
            append(callableName)
        }
    }
}

fun CallableId.withClassId(classId: ClassId): CallableId {
    return CallableId(classId, callableName)
}
