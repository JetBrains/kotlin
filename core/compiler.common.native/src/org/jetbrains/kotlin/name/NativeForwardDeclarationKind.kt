/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.name

import org.jetbrains.kotlin.descriptors.ClassKind

/**
 * Enum class representing different kinds of synthetic forward declarations.
 *
 * @property packageFqName The fully qualified name of the package where the forward declarations of this kind are located.
 * @property superClassName The name of the generated superclass of the forward declaration.
 * @property matchSuperClassName The name of the superclass real declaration corresponding to this synthetic should have.
 * @property classKind The class kind of the forward declaration class (class or interface).
 *
 * Also, fqName and classId of super classes are stored as optimization to avoid allocations and string processing on usage.
 */
enum class NativeForwardDeclarationKind(val packageFqName: FqName, val superClassName: Name, val matchSuperClassName: Name, val classKind: ClassKind) {
    Struct(
        NativeStandardInteropNames.ForwardDeclarations.cNamesStructsPackage,
        NativeStandardInteropNames.COpaque,
        NativeStandardInteropNames.CStructVar,
        ClassKind.CLASS
    ),
    ObjCClass(
        NativeStandardInteropNames.ForwardDeclarations.objCNamesClassesPackage,
        NativeStandardInteropNames.ObjCObjectBase,
        NativeStandardInteropNames.ObjCObjectBase,
        ClassKind.CLASS
    ),
    ObjCProtocol(
        NativeStandardInteropNames.ForwardDeclarations.objCNamesProtocolsPackage,
        NativeStandardInteropNames.ObjCObject,
        NativeStandardInteropNames.ObjCObject,
        ClassKind.INTERFACE
    )
    ;

    val superClassFqName = NativeStandardInteropNames.cInteropPackage.child(superClassName)
    val matchSuperClassFqName = NativeStandardInteropNames.cInteropPackage.child(matchSuperClassName)
    val superClassId = ClassId.topLevel(superClassFqName)
    val matchSuperClassId = ClassId.topLevel(matchSuperClassFqName)

    companion object {
        val packageFqNameToKind: Map<FqName, NativeForwardDeclarationKind> = NativeForwardDeclarationKind.entries.associateBy { it.packageFqName }
    }
}