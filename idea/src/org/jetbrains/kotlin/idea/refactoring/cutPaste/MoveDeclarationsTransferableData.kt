/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.refactoring.cutPaste

import com.intellij.codeInsight.editorActions.TextBlockTransferableData
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import java.awt.datatransfer.DataFlavor

class MoveDeclarationsTransferableData(
        val sourceFileUrl: String,
        val sourceObjectFqName: String?,
        val stubTexts: List<String>,
        val declarationNames: Set<String>
) : TextBlockTransferableData {

    override fun getFlavor() = DATA_FLAVOR
    override fun getOffsetCount() = 0

    override fun getOffsets(offsets: IntArray?, index: Int) = index
    override fun setOffsets(offsets: IntArray?, index: Int) = index

    companion object {
        val DATA_FLAVOR = DataFlavor(MoveDeclarationsCopyPasteProcessor::class.java, "class: MoveDeclarationsCopyPasteProcessor")

        val STUB_RENDERER = IdeDescriptorRenderers.SOURCE_CODE.withOptions {
            defaultParameterValueRenderer = { "xxx" } // we need default value to be parsed as expression
        }
    }
}