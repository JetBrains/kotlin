/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.name

// NB: with className == null we are at top level
data class CallableId(
    val packageName: FqName,
    val className: FqName?,
    val callableName: Name,
    // Currently, it's only used for debug info
    private val pathToLocal: FqName? = null
) {
    private companion object {
        val LOCAL_NAME = Name.special("<local>")
        val PACKAGE_FQ_NAME_FOR_LOCAL = FqName.topLevel(LOCAL_NAME)
    }

    var classId: ClassId? = null
        get() {
            if (field == null && className != null) {
                field = ClassId(packageName, className, false)
            }
            return field
        }

    constructor(classId: ClassId, callableName: Name) : this(classId.packageFqName, classId.relativeClassName, callableName) {
        this.classId = classId
    }

    constructor(packageName: FqName, callableName: Name) : this(packageName, null, callableName)

    @LocalCallableIdConstructor
    constructor(
        callableName: Name,
        // Currently, it's only used for debug info
        pathToLocal: FqName? = null
    ) : this(
        PACKAGE_FQ_NAME_FOR_LOCAL,
        className = null,
        callableName,
        pathToLocal,
    )

    fun asFqNameForDebugInfo(): FqName {
        if (pathToLocal != null) return pathToLocal.child(callableName)

        return classId?.asSingleFqName()?.child(callableName) ?: packageName.child(callableName)
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

@RequiresOptIn("TODO: Better solution for local callables?")
annotation class LocalCallableIdConstructor