/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

enum class ImplementationKind(val title: String, val typeKind: TypeKind) {
    Interface("interface", TypeKind.Interface),
    FinalClass("class", TypeKind.Class),
    OpenClass("open class", TypeKind.Class),
    AbstractClass("abstract class", TypeKind.Class),
    SealedClass("sealed class", TypeKind.Class),
    SealedInterface("sealed interface", TypeKind.Interface),
    Object("object", TypeKind.Class);

    val isInterface: Boolean
        get() = typeKind == TypeKind.Interface
}