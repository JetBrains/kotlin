/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.lazy

import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolver

interface IrLazyClassBase : IrLazyDeclarationBase {
    val moduleName: String?
        get() = null

    val firModuleName: String?
        get() = null

    val protobuf: Pair<ProtoBuf.Class, NameResolver>?
        get() = null

    val isNewPlaceForBodyGeneration: Boolean?
        get() = null

    val isK2: Boolean
}
