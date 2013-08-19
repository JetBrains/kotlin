/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.java.structure.impl;

import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.util.PsiFormatUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.java.structure.JavaMethod;
import org.jetbrains.jet.lang.resolve.java.structure.JavaSignatureFormatter;

import static com.intellij.psi.util.PsiFormatUtilBase.*;

public class JavaSignatureFormatterImpl extends JavaSignatureFormatter {
    @NotNull
    @Override
    public String formatMethod(@NotNull JavaMethod method) {
        return PsiFormatUtil.formatMethod(method.getPsi(), PsiSubstitutor.EMPTY, SHOW_NAME | SHOW_PARAMETERS,
                                          SHOW_TYPE | SHOW_FQ_CLASS_NAMES);
    }

    @NotNull
    @Override
    public String getExternalName(@NotNull JavaMethod method) {
        String result = PsiFormatUtil.getExternalName(method.getPsi());
        return result == null ? "null" : result;
    }
}
