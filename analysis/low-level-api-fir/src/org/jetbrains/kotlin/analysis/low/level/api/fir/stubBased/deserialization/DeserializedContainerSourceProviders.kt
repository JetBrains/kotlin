/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.stubs.impl.KotlinStubOrigin
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

internal interface DeserializedContainerSourceProvider {
    fun getContainerSource(file: KtFile, origin: KotlinStubOrigin?): DeserializedContainerSource?
}

// Currently, `null` is returned for KLIBs to avoid incorrect application of JVM file facade logic and overload filtering.
// We might want to provide non-`null` container source for all types of binaries in the future.
internal object NoSourceDeserializedContainerSourceProvider : DeserializedContainerSourceProvider {
    override fun getContainerSource(file: KtFile, origin: KotlinStubOrigin?): DeserializedContainerSource? = null
}

internal object JvmDeserializedContainerSourceProvider : DeserializedContainerSourceProvider {
    override fun getContainerSource(file: KtFile, origin: KotlinStubOrigin?): DeserializedContainerSource {
        return when (origin) {
            is KotlinStubOrigin.Facade -> {
                val className = JvmClassName.byInternalName(origin.className)
                JvmStubDeserializedFacadeContainerSource(className, facadeClassName = null)
            }
            is KotlinStubOrigin.MultiFileFacade -> {
                val className = JvmClassName.byInternalName(origin.className)
                val facadeClassName = JvmClassName.byInternalName(origin.facadeClassName)
                JvmStubDeserializedFacadeContainerSource(className, facadeClassName)
            }
            else -> {
                val virtualFile = file.virtualFile
                val classId = ClassId(file.packageFqName, Name.identifier(virtualFile.nameWithoutExtension))
                val className = JvmClassName.byClassId(classId)
                JvmStubDeserializedFacadeContainerSource(className, facadeClassName = null)
            }
        }
    }
}

internal object BuiltinsDeserializedContainerSourceProvider : DeserializedContainerSourceProvider {
    override fun getContainerSource(file: KtFile, origin: KotlinStubOrigin?): DeserializedContainerSource? {
        require(origin is KotlinStubOrigin.Facade) {
            "Expected builtins file to have Facade origin, got origin=$origin instead"
        }

        return JvmStubDeserializedBuiltInsContainerSource(
            facadeClassName = JvmClassName.byInternalName(origin.className)
        )
    }
}
