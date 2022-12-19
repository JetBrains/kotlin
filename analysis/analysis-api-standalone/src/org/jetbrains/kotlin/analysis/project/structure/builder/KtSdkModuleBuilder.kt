/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure.builder

import org.jetbrains.kotlin.analysis.project.structure.KtSdkModule
import org.jetbrains.kotlin.analysis.project.structure.impl.KtSdkModuleImpl
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@KtModuleBuilderDsl
public class KtSdkModuleBuilder : KtBinaryModuleBuilder() {
    public lateinit var sdkName: String

    override fun build(): KtSdkModule {
        return KtSdkModuleImpl(
            directRegularDependencies,
            directDependsOnDependencies,
            directFriendDependencies,
            contentScope,
            platform,
            project,
            binaryRoots,
            sdkName
        )
    }
}

@OptIn(ExperimentalContracts::class)
public inline fun buildKtSdkModule(init: KtSdkModuleBuilder.() -> Unit): KtSdkModule {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return KtSdkModuleBuilder().apply(init).build()
}