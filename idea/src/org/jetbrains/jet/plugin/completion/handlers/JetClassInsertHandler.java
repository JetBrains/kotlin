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

package org.jetbrains.jet.plugin.completion.handlers;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiDocumentManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.plugin.codeInsight.ShortenReferences;
import org.jetbrains.jet.plugin.completion.JetLookupObject;

public class JetClassInsertHandler implements InsertHandler<LookupElement> {

    public static final InsertHandler<LookupElement> INSTANCE = new JetClassInsertHandler();

    @Override
    public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement item) {
        if (context.getFile() instanceof JetFile) {
            if (item.getObject() instanceof JetLookupObject) {
                JetLookupObject lookupObject = (JetLookupObject)item.getObject();
                DeclarationDescriptor descriptor = lookupObject.getDescriptor();
                if (descriptor != null) {
                    int startOffset = context.getStartOffset();
                    Document document = context.getDocument();
                    if (!isAfterDot(document, startOffset)) {
                        String text = DescriptorUtils.getFqName(descriptor).asString();
                        document.replaceString(startOffset, context.getTailOffset(), text);

                        PsiDocumentManager.getInstance(context.getProject()).commitAllDocuments();
                        ShortenReferences.instance$.process((JetFile) context.getFile(), startOffset, startOffset + text.length());
                    }
                }
            }
        }
    }

    private static boolean isAfterDot(Document document, int offset) {
        CharSequence chars = document.getCharsSequence();
        while(offset > 0) {
            offset--;
            char c = chars.charAt(offset);
            if (!Character.isWhitespace(c)) {
                return c == '.';
            }
        }
        return false;
    }
}
