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

package org.jetbrains.jet.plugin.completion.weigher;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementWeigher;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.ImportPath;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.plugin.completion.JetLookupObject;
import org.jetbrains.jet.plugin.quickfix.ImportInsertHelper;
import org.jetbrains.jet.util.QualifiedNamesUtil;

public class JetDeclarationRemotenessWeigher extends LookupElementWeigher {
    private final JetFile file;

    protected JetDeclarationRemotenessWeigher(JetFile file) {
        super(JetDeclarationRemotenessWeigher.class.getSimpleName());
        this.file = file;
    }

    private enum MyResult {
        kotlinDefaultImport,
        thisFile,
        imported,
        normal,
        notImported,
    }

    @NotNull
    @Override
    public Comparable weigh(@NotNull LookupElement element) {
        Object object = element.getObject();
        if (object instanceof JetLookupObject) {
            JetLookupObject lookupObject = (JetLookupObject) object;

            PsiElement psiElement = lookupObject.getPsiElement();
            if (psiElement != null) {
                PsiFile elementFile = psiElement.getContainingFile();
                if (elementFile instanceof JetFile && elementFile.getOriginalFile() == file) {
                    return MyResult.thisFile;
                }
            }

            DeclarationDescriptor descriptor = lookupObject.getDescriptor();
            if (descriptor != null) {
                FqNameUnsafe fqName = DescriptorUtils.getFqName(descriptor);
                // Invalid name can be met for class object descriptor: Test.MyTest.A.<no name provided>.testOther
                if (QualifiedNamesUtil.isValidJavaFqName(fqName.toString())) {
                    ImportPath importPath = new ImportPath(fqName.toString());
                    if (ImportInsertHelper.doNeedImport(importPath, file)) {
                        return MyResult.notImported;
                    }
                    else {
                        if (ImportInsertHelper.isImportedWithDefault(importPath, file)) {
                            return MyResult.kotlinDefaultImport;
                        }
                        return MyResult.imported;
                    }
                }
            }
        }

        return MyResult.normal;
    }
}
