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

package org.jetbrains.jet.plugin.hierarchy;

import com.intellij.ide.hierarchy.HierarchyBrowserBaseEx;
import com.intellij.ide.hierarchy.HierarchyTreeStructure;
import com.intellij.ide.hierarchy.LanguageTypeHierarchy;
import com.intellij.ide.hierarchy.type.SubtypesHierarchyTreeStructure;
import com.intellij.ide.hierarchy.type.SupertypesHierarchyTreeStructure;
import com.intellij.ide.hierarchy.type.TypeHierarchyTreeStructure;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.MapDataContext;
import com.intellij.testFramework.codeInsight.hierarchy.HierarchyViewTestBase;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
Test Hierarchy view
Format: test build hierarchy for element at caret, file with caret should be the first in the sorted list of files.
Test accept more than one file, file extension should be .java or .kt
 */
public abstract class AbstractHierarchyTest extends HierarchyViewTestBase {

    private String folderName;

    protected void doTypeClassHierarchyTest(@NotNull String folderName) throws Exception {
        this.folderName = folderName;
        doHierarchyTest(getTypeHierarchyStructure(), getFilesToConfigure());
    }

    protected void doSuperClassHierarchyTest(@NotNull String folderName) throws Exception {
        this.folderName = folderName;
        doHierarchyTest(getSuperTypesHierarchyStructure(), getFilesToConfigure());
    }

    protected void doSubClassHierarchyTest(@NotNull String folderName) throws Exception {
        this.folderName = folderName;
        doHierarchyTest(getSubTypesHierarchyStructure(), getFilesToConfigure());
    }

    private Computable<HierarchyTreeStructure> getSuperTypesHierarchyStructure() {
        return new Computable<HierarchyTreeStructure>() {
            @Override
            public HierarchyTreeStructure compute() {
                return new SupertypesHierarchyTreeStructure(getProject(), (PsiClass) getElementAtCaret());
            }
        };
    }

    private Computable<HierarchyTreeStructure> getSubTypesHierarchyStructure() {
        return new Computable<HierarchyTreeStructure>() {
            @Override
            public HierarchyTreeStructure compute() {
                return new SubtypesHierarchyTreeStructure(getProject(), (PsiClass) getElementAtCaret(),
                                                          HierarchyBrowserBaseEx.SCOPE_PROJECT);
            }
        };
    }

    private Computable<HierarchyTreeStructure> getTypeHierarchyStructure() {
        return new Computable<HierarchyTreeStructure>() {
            @Override
            public HierarchyTreeStructure compute() {
                return new TypeHierarchyTreeStructure(getProject(), (PsiClass) getElementAtCaret(),
                                                      HierarchyBrowserBaseEx.SCOPE_PROJECT);
            }
        };
    }

    private PsiElement getElementAtCaret() {
        PsiElement target = LanguageTypeHierarchy.INSTANCE.forLanguage(getLanguage()).getTarget(getDataContext());
        assert target != null : "Cannot apply action for element at caret";
        return target;
    }

    private Language getLanguage() {
        PsiFile file = PsiDocumentManager.getInstance(getProject()).getPsiFile(getEditor().getDocument());
        assert file != null : "Cannot find file in editor";
        return file.getLanguage();
    }

    private DataContext getDataContext() {
        MapDataContext context = new MapDataContext();
        context.put(PlatformDataKeys.PROJECT, getProject());
        context.put(PlatformDataKeys.EDITOR, getEditor());
        return context;
    }

    private String[] getFilesToConfigure() {
        final List<String> files = new ArrayList<String>(2);
        FileUtil.processFilesRecursively(new File(folderName), new Processor<File>() {
            @Override
            public boolean process(File file) {
                String fileName = file.getName();
                if (fileName.endsWith(".kt") || fileName.endsWith(".java")) {
                    files.add(fileName);
                }
                return true;
            }
        });
        Collections.sort(files);
        return files.toArray(new String[files.size()]);
    }

    @Override
    protected String getBasePath() {
        return folderName.substring("idea/testData/".length());
    }

    @Override
    protected String getTestDataPath() {
        return PluginTestCaseBase.getTestDataPathBase();
    }

    @Override
    protected Sdk getTestProjectJdk() {
        return PluginTestCaseBase.jdkFromIdeaHome();
    }
}
