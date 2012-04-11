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

package org.jetbrains.jet.plugin.completion.handlers;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiElement;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.FqName;
import org.jetbrains.jet.plugin.completion.JetLookupObject;
import org.jetbrains.jet.plugin.quickfix.ImportInsertHelper;

/**
 * @author Nikolay Krasko
 */
public class JetClassInsertHandler implements InsertHandler<LookupElement> {

    public static final InsertHandler<LookupElement> INSTANCE = new JetClassInsertHandler();

    @Override
    public void handleInsert(final InsertionContext context, final LookupElement item) {
        if (context.getFile() instanceof JetFile) {
            final JetFile jetFile = (JetFile) context.getFile();
            ApplicationManager.getApplication().runReadAction(new Runnable() {
                @Override
                public void run() {
                    int startOffset = context.getStartOffset();
                    PsiElement element = context.getFile().findElementAt(startOffset);
                    if (element == null) {
                        return;
                    }

                    if (context.getFile() instanceof JetFile && item.getObject() instanceof JetLookupObject) {
                        final DeclarationDescriptor descriptor = ((JetLookupObject) item.getObject()).getDescriptor();
                        if (descriptor != null) {
                            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                                @Override
                                public void run() {
                                    final FqName fqn = DescriptorUtils.getFQName(descriptor).toSafe();
                                    ImportInsertHelper.addImportDirective(fqn, jetFile);
                                }
                            });
                        }
                    }
                }
            });
        }
    }
}
