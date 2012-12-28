/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.conversion.copy;

import com.intellij.codeInsight.editorActions.TextBlockTransferableData;
import org.jetbrains.annotations.NotNull;

import java.awt.datatransfer.DataFlavor;

class ConvertedCode implements TextBlockTransferableData {
    @NotNull
    public static final DataFlavor DATA_FLAVOR = new DataFlavor(JavaCopyPastePostProcessor.class, "class: JavaCopyPastePostProcessor");
    private final String data;

    ConvertedCode(String data) {
        this.data = data;
    }

    @NotNull
    @Override
    public DataFlavor getFlavor() {
        return DATA_FLAVOR;
    }

    @Override
    public int getOffsetCount() {
        return 0;
    }

    @Override
    public int getOffsets(int[] offsets, int index) {
        return 0;
    }

    @Override
    public int setOffsets(int[] offsets, int index) {
        return 0;
    }

    public String getData() {
        return data;
    }
}
