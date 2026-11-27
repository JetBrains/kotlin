/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("IrDeclarationBuilders")
@file:JvmMultifileClass

package org.jetbrains.kotlin.ir.builders.declarations.v2

import org.jetbrains.kotlin.ir.declarations.IrParameterKind

@DslMarker
annotation class IrDeclarationBuilderDsl

internal val IrDeclarationBuilder.nameForMessages: String
    get() = when {
        this is IrDeclarationWithNameBuilder && isNameInitialized -> name.asString()
        this is IrAnonymousInitializerBuilder -> "<anonymous initializer>"
        else -> "<unnamed>"
    }

internal val IrFunctionBuilder.hasDispatchReceiver: Boolean
    get() = parameters.any { it.kind == IrParameterKind.DispatchReceiver }
