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

import com.intellij.codeInsight.editorActions.*
import java.io.*
import java.awt.datatransfer.*
import kotlin.properties.*

public class KotlinReferenceTransferableData(
        val data: Array<KotlinReferenceData>
) : TextBlockTransferableData, Cloneable, Serializable {

    override fun getFlavor() = KotlinReferenceData.dataFlavor

    override fun getOffsetCount() = data.size() * 2

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

    public override fun clone() = KotlinReferenceTransferableData(Array(data.size(), {  data[it].clone() }))
}

public class KotlinReferenceData(
        public var startOffset: Int,
        public var endOffset: Int,
        public val fqName: String,
        public val kind: KotlinReferenceData.Kind
) : Cloneable, Serializable {

    public enum class Kind {
        CLASS
        PACKAGE
        NON_EXTENSION_CALLABLE
        EXTENSION_CALLABLE
    }

    public override fun clone(): KotlinReferenceData {
        try {
            return super<Cloneable>.clone() as KotlinReferenceData
        }
        catch (e: CloneNotSupportedException) {
            throw RuntimeException()
        }
    }

    default object {
        public val dataFlavor: DataFlavor? by Delegates.lazy {
            try {
                DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=" + javaClass<KotlinReferenceData>().getName(), "KotlinReferenceData")
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