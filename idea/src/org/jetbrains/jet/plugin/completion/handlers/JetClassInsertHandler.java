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
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.plugin.completion.JetLookupObject;
import org.jetbrains.jet.plugin.project.ProjectStructureUtil;
import org.jetbrains.jet.plugin.quickfix.ImportInsertHelper;

public class JetClassInsertHandler implements InsertHandler<LookupElement> {

    public static final InsertHandler<LookupElement> INSTANCE = new JetClassInsertHandler();

    @Override
    public void handleInsert(final InsertionContext context, final LookupElement item) {
        PsiDocumentManager.getInstance(context.getProject()).commitAllDocuments();

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
                        JetLookupObject lookupObject = (JetLookupObject)item.getObject();
                        DeclarationDescriptor descriptor = lookupObject.getDescriptor();
                        PsiElement targetElement = lookupObject.getPsiElement();
                        if (descriptor != null) {
                            FqName fqn = DescriptorUtils.getFqNameSafe(descriptor);

                            // TODO: Find out the way for getting psi element for JS libs
                            if (targetElement != null) {
                                ImportInsertHelper.addImportDirectiveOrChangeToFqName(fqn, jetFile, context.getStartOffset(), targetElement);
                            }
                            else if (ProjectStructureUtil.isJsKotlinModule(jetFile)) {
                                ImportInsertHelper.addImportDirectiveIfNeeded(fqn, jetFile);
                            }
                        }
                    }
                }
            });
        }
    }
}
