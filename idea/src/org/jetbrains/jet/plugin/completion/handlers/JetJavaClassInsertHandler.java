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
import com.intellij.codeInsight.completion.JavaPsiClassReferenceElement;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.FqName;
import org.jetbrains.jet.plugin.quickfix.ImportInsertHelper;

/**
 * Handler for inserting java class completion.
 * - Should place import directive if necessary.
 *
 * @author Nikolay Krasko
 */
public class JetJavaClassInsertHandler implements InsertHandler<JavaPsiClassReferenceElement> {
    public static final InsertHandler<JavaPsiClassReferenceElement> JAVA_CLASS_INSERT_HANDLER = new JetJavaClassInsertHandler();

    @Override
    public void handleInsert(final InsertionContext context, final JavaPsiClassReferenceElement item) {
        if (context.getFile() instanceof JetFile) {
            ImportInsertHelper.addImportDirectiveOrChangeToFqName(new FqName(item.getQualifiedName()),
                                                                  (JetFile) context.getFile(),
                                                                  context.getStartOffset(),
                                                                  item.getObject());
        }

        // check annotation
        // check auto insert parentheses
        // check auto insert type parameters
    }
}
