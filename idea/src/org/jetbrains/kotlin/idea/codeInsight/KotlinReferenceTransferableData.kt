/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.codeInsight

import com.intellij.codeInsight.editorActions.TextBlockTransferableData
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.descriptorUtil.getImportableDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import java.awt.datatransfer.DataFlavor
import java.io.Serializable

class KotlinReferenceTransferableData(
        val data: Array<KotlinReferenceData>
) : TextBlockTransferableData, Cloneable, Serializable {

    override fun getFlavor() = KotlinReferenceData.dataFlavor

    override fun getOffsetCount() = data.size * 2

    override fun getOffsets(offsets: IntArray, index: Int): Int {
        var i = index
        for (d in data) {
            offsets[i++] = d.startOffset
            offsets[i++] = d.endOffset
        }
        return i
    }

    override fun setOffsets(offsets: IntArray, index: Int): Int {
        var i = index
        for (d in data) {
            d.startOffset = offsets[i++]
            d.endOffset = offsets[i++]
        }
        return i
    }

    public override fun clone() = KotlinReferenceTransferableData(Array(data.size, {  data[it].clone() }))
}

class KotlinReferenceData(
        var startOffset: Int,
        var endOffset: Int,
        val fqName: String,
        val kind: KotlinReferenceData.Kind
) : Cloneable, Serializable {

    enum class Kind {
        CLASS,
        PACKAGE,
        NON_EXTENSION_CALLABLE,
        EXTENSION_FUNCTION,
        EXTENSION_PROPERTY;

        companion object {
            fun fromDescriptor(descriptor: DeclarationDescriptor): KotlinReferenceData.Kind? {
                return when (descriptor.getImportableDescriptor()) {
                    is ClassDescriptor ->
                        KotlinReferenceData.Kind.CLASS

                    is PackageViewDescriptor ->
                        KotlinReferenceData.Kind.PACKAGE

                    is FunctionDescriptor ->
                        if (descriptor.isExtension) KotlinReferenceData.Kind.EXTENSION_FUNCTION else KotlinReferenceData.Kind.NON_EXTENSION_CALLABLE

                    is PropertyDescriptor ->
                        if (descriptor.isExtension) KotlinReferenceData.Kind.EXTENSION_PROPERTY else KotlinReferenceData.Kind.NON_EXTENSION_CALLABLE

                    else ->
                        null
                }
            }
        }
    }

    public override fun clone(): KotlinReferenceData {
        try {
            return super.clone() as KotlinReferenceData
        }
        catch (e: CloneNotSupportedException) {
            throw RuntimeException()
        }
    }

    companion object {
        val dataFlavor: DataFlavor? by lazy {
            try {
                val dataClass = KotlinReferenceData::class.java
                DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=" + dataClass.name,
                           "KotlinReferenceData",
                           dataClass.classLoader)
            }
            catch (e: NoClassDefFoundError) {
                null
            }
            catch (e: IllegalArgumentException) {
                null
            }
        }
    }
}