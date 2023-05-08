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
    companion object {
        private val LOCAL_NAME = SpecialNames.LOCAL
        val PACKAGE_FQ_NAME_FOR_LOCAL = FqName.topLevel(LOCAL_NAME)
    }

    /**
     * Return `true` if corresponding declaration is itself local or it is a member of local class
     * Otherwise, returns `false`
     */
    val isLocal: Boolean
        get() = packageName == PACKAGE_FQ_NAME_FOR_LOCAL || classId?.isLocal == true

    var classId: ClassId? = null
        get() {
            if (field == null && className != null) {
                field = ClassId(packageName, className, packageName == PACKAGE_FQ_NAME_FOR_LOCAL)
            }
            return field
        }
        private set

    constructor(classId: ClassId, callableName: Name) : this(classId.packageFqName, classId.relativeClassName, callableName) {
        this.classId = classId
    }

    constructor(packageName: FqName, callableName: Name) : this(packageName, null, callableName)

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
        return asSingleFqName()
    }

    fun asSingleFqName(): FqName {
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
