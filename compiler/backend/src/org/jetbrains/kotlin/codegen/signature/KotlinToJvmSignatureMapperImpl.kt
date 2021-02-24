/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.signature

import org.jetbrains.kotlin.codegen.ClassBuilderMode
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.KotlinToJvmSignatureMapper

class KotlinToJvmSignatureMapperImpl : KotlinToJvmSignatureMapper {
    // We use empty BindingContext, because it is only used by KotlinTypeMapper for purposes irrelevant to the needs of this class
    private val typeMapper = KotlinTypeMapper(
        BindingContext.EMPTY, ClassBuilderMode.LIGHT_CLASSES,
        JvmProtoBufUtil.DEFAULT_MODULE_NAME,
        KotlinTypeMapper.LANGUAGE_VERSION_SETTINGS_DEFAULT,// TODO use proper LanguageVersionSettings
        useOldInlineClassesManglingScheme = false
    )

    override fun mapToJvmMethodSignature(function: FunctionDescriptor) = typeMapper.mapAsmMethod(function)
}
