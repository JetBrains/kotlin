/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream

sealed class KotlinStubOrigin {
    companion object {
        private const val FACADE_KIND = 1
        private const val MULTI_FILE_FACADE_KIND = 2

        @JvmStatic
        fun serialize(origin: KotlinStubOrigin?, dataStream: StubOutputStream) {
            if (origin == null) {
                dataStream.writeInt(0)
            } else {
                dataStream.writeInt(origin.kind)
                origin.serializeContent(dataStream)
            }
        }

        @JvmStatic
        fun deserialize(dataStream: StubInputStream): KotlinStubOrigin? {
            return when (dataStream.readInt()) {
                FACADE_KIND -> Facade.deserializeContent(dataStream)
                MULTI_FILE_FACADE_KIND -> MultiFileFacade.deserializeContent(dataStream)
                else -> null
            }
        }
    }

    protected abstract val kind: Int

    protected abstract fun serializeContent(dataStream: StubOutputStream)

    data class Facade(
        val className: String // Internal name of the package part class
    ) : KotlinStubOrigin() {
        companion object {
            @JvmStatic
            internal fun deserializeContent(dataStream: StubInputStream): Facade? {
                val className = dataStream.readNameString() ?: return null
                return Facade(className)
            }
        }

        override val kind: Int get() = FACADE_KIND

        override fun serializeContent(dataStream: StubOutputStream) {
            dataStream.writeName(className)
        }
    }

    data class MultiFileFacade(
        val className: String, // Internal name of the package part class
        val facadeClassName: String // Internal name of the facade class
    ) : KotlinStubOrigin() {
        companion object {
            @JvmStatic
            internal fun deserializeContent(dataStream: StubInputStream): MultiFileFacade? {
                val classId = dataStream.readNameString() ?: return null
                val facadeClassId = dataStream.readNameString() ?: return null
                return MultiFileFacade(classId, facadeClassId)
            }
        }

        override val kind: Int get() = MULTI_FILE_FACADE_KIND

        override fun serializeContent(dataStream: StubOutputStream) {
            dataStream.writeName(className)
            dataStream.writeName(facadeClassName)
        }
    }
}