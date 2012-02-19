/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin;

import com.intellij.ide.highlighter.JavaClassFileType;
import com.intellij.lang.Language;
import com.intellij.openapi.fileTypes.ContentBasedClassFileProcessor;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.impl.compiled.ClassFileStubBuilder;
import com.intellij.psi.impl.compiled.ClsAnnotationImpl;
import com.intellij.psi.impl.java.stubs.PsiJavaFileStub;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Arrays;

/**
 * @author Evgeny Gerashchenko
 * @since 2/15/12
 */
public class JetContentBasedFileSubstitutor implements ContentBasedClassFileProcessor {

    @Override
    public boolean isApplicable(Project project, VirtualFile vFile) {
        return isKotlinClass(project, vFile);
    }

    @NotNull
    @Override
    public String obtainFileText(Project project, VirtualFile file) {
        return file.getNameWithoutExtension();
    }

    @Override
    public Language obtainLanguageForFile(VirtualFile file) {
        if (!file.getNameWithoutExtension().contains("$")) {
            return JetLanguage.INSTANCE;
        }
        return null;
    }

    @NotNull
    @Override
    public SyntaxHighlighter createHighlighter(Project project, VirtualFile vFile) {
        return JetHighlighter.PROVIDER.create(JetFileType.INSTANCE, project, vFile);
    }

    private static boolean isKotlinClass(Project project, VirtualFile file) {
        if (!FileTypeManager.getInstance().isFileOfType(file, JavaClassFileType.INSTANCE)) {
            return false;
        }
        PsiJavaFileStub javaStub = getJavaStub(project, file);
        if (javaStub == null) {
            return false;
        }
        // TODO multiple?
        if (javaStub.getClasses().length != 1) {
            throw new AssertionError("Multiple classes in file: " + Arrays.toString(javaStub.getClasses()));
        }
        PsiClass psiClass = javaStub.getClasses()[0];
        // TODO add better check
        if (psiClass.getName().equals("namespace")) {
            return true;
        }
        PsiModifierList modifierList = psiClass.getModifierList();
        if (modifierList != null) {
            for (PsiAnnotation annotation : modifierList.getAnnotations()) {
                if (annotation instanceof ClsAnnotationImpl) {
                    if (((ClsAnnotationImpl) annotation).getStub().getText().startsWith("@jet.runtime.typeinfo.JetClass")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static PsiJavaFileStub getJavaStub(Project project, VirtualFile file) {
        try {
            byte[] bytes = StreamUtil.loadFromStream(file.getInputStream());
            return (PsiJavaFileStub) new ClassFileStubBuilder().buildStubTree(file, bytes, project);
        } catch (IOException e) {
            return null;
        }
    }


}
- 
