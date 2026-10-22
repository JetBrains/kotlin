/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(KtImplementationDetail::class)

package org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization

import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.stubs.impl.KotlinClassStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.KotlinObjectStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.KotlinStubOrigin
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

internal interface DeserializedContainerSourceProvider {
    fun getFacadeContainerSource(
        file: KtFile,
        stubOrigin: KotlinStubOrigin?,
        declarationOrigin: FirDeclarationOrigin,
    ): DeserializedContainerSource?

    fun getClassContainerSource(classId: ClassId, declaration: KtClassOrObject): DeserializedContainerSource?
}

// Currently, `null` is returned for KLIBs to avoid incorrect application of JVM file facade logic and overload filtering.
// We might want to provide non-`null` container source for all types of binaries in the future.
internal object NullDeserializedContainerSourceProvider : DeserializedContainerSourceProvider {
    override fun getFacadeContainerSource(
        file: KtFile,
        stubOrigin: KotlinStubOrigin?,
        declarationOrigin: FirDeclarationOrigin,
    ): DeserializedContainerSource? = null

    override fun getClassContainerSource(classId: ClassId, declaration: KtClassOrObject): DeserializedContainerSource? = null
}

internal object JvmDeserializedContainerSourceProvider : DeserializedContainerSourceProvider {
    override fun getFacadeContainerSource(
        file: KtFile,
        stubOrigin: KotlinStubOrigin?,
        declarationOrigin: FirDeclarationOrigin,
    ): DeserializedContainerSource {
        return when (stubOrigin) {
            is KotlinStubOrigin.Facade -> {
                val className = JvmClassName.byInternalName(stubOrigin.className)
                val jvmClassName = if (stubOrigin.jvmClassName != null) JvmClassName.byInternalName(stubOrigin.jvmClassName!!) else null
                JvmStubDeserializedFacadeContainerSource(
                    className = className,
                    jvmClassName = jvmClassName,
                    facadeClassName = null,
                    classFileMajorVersion = stubOrigin.classFileMajorVersion,
                )
            }
            is KotlinStubOrigin.MultiFileFacade -> {
                val className = JvmClassName.byInternalName(stubOrigin.className)
                val facadeClassName = JvmClassName.byInternalName(stubOrigin.facadeClassName)
                JvmStubDeserializedFacadeContainerSource(
                    className = className,
                    jvmClassName = null,
                    facadeClassName = facadeClassName,
                    classFileMajorVersion = stubOrigin.classFileMajorVersion,
                )
            }
            else -> {
                val virtualFile = file.virtualFile
                val classId = ClassId(file.packageFqName, Name.identifier(virtualFile.nameWithoutExtension))
                val className = JvmClassName.byClassId(classId)
                JvmStubDeserializedFacadeContainerSource(
                    className = className,
                    jvmClassName = null,
                    facadeClassName = null,
                    classFileMajorVersion = null,
                )
            }
        }
    }

    override fun getClassContainerSource(classId: ClassId, declaration: KtClassOrObject): DeserializedContainerSource? {
        return JvmStubDeserializedContainerSource(classId, declaration.classFileMajorVersion)
    }
}

internal object BuiltinsDeserializedContainerSourceProvider : DeserializedContainerSourceProvider {
    override fun getFacadeContainerSource(
        file: KtFile,
        stubOrigin: KotlinStubOrigin?,
        declarationOrigin: FirDeclarationOrigin,
    ): DeserializedContainerSource {
        require(stubOrigin is KotlinStubOrigin.Facade) {
            "Expected builtins file to have Facade origin, got origin=$stubOrigin instead"
        }

        return JvmStubDeserializedBuiltInsContainerSource(
            facadeClassName = JvmClassName.byInternalName(stubOrigin.className)
        )
    }

    override fun getClassContainerSource(classId: ClassId, declaration: KtClassOrObject): DeserializedContainerSource? {
        return JvmStubDeserializedContainerSource(classId, declaration.classFileMajorVersion)
    }
}

internal object JvmAndBuiltinsDeserializedContainerSourceProvider : DeserializedContainerSourceProvider {
    override fun getFacadeContainerSource(
        file: KtFile,
        stubOrigin: KotlinStubOrigin?,
        declarationOrigin: FirDeclarationOrigin
    ): DeserializedContainerSource? {
        if (declarationOrigin is FirDeclarationOrigin.BuiltIns) {
            return BuiltinsDeserializedContainerSourceProvider.getFacadeContainerSource(file, stubOrigin, declarationOrigin)
        }

        return JvmDeserializedContainerSourceProvider.getFacadeContainerSource(file, stubOrigin, declarationOrigin)
    }

    override fun getClassContainerSource(classId: ClassId, declaration: KtClassOrObject): DeserializedContainerSource? {
        return JvmStubDeserializedContainerSource(classId, declaration.classFileMajorVersion)
    }
}

private val KtClassOrObject.classFileMajorVersion: Int?
    get() = when (this) {
        is KtClass -> {
            val classStub: KotlinClassStubImpl = compiledStub
            classStub.classFileMajorVersion
        }

        is KtObjectDeclaration -> {
            val objectStub: KotlinObjectStubImpl = compiledStub
            objectStub.classFileMajorVersion
        }

        else -> null
    }
