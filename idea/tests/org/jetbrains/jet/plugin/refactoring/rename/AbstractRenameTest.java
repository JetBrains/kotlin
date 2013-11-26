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

package org.jetbrains.jet.plugin.refactoring.rename;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.refactoring.MultiFileTestCase;
import com.intellij.refactoring.rename.RenameProcessor;
import com.intellij.refactoring.rename.RenamePsiElementProcessor;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.util.Function;
import junit.framework.Assert;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.jet.InTextDirectivesUtils;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.jetbrains.jet.plugin.PluginTestCaseBase;
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache;
import org.jetbrains.jet.utils.ExceptionUtils;

import java.io.File;
import java.util.Collections;

public abstract class AbstractRenameTest extends MultiFileTestCase {
    private enum RenameType {
        JAVA_CLASS,
        JAVA_METHOD,
        KOTLIN_CLASS,
        KOTLIN_FUNCTION,
        KOTLIN_PROPERTY,
    }

    public void doTest(String path) {
        try {
            String fileText = FileUtil.loadFile(new File(path));
            String renameDirective = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// RENAME:");
            Assert.assertNotNull("'// RENAME:' directive is expected for rename test file", renameDirective);

            String[] strings = renameDirective.split("->");
            Assert.assertTrue("'// RENAME:' directive should have at least AbstractRenameTest.RenameType parameter", strings.length > 0);

            String hintDirective = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// HINT:");

            String renameTypeStr = strings[0];
            RenameType type = RenameType.valueOf(renameTypeStr);

            try {
                FqNameUnsafe fqNameUnsafe = new FqNameUnsafe(strings[1]);

                switch (type) {
                    case JAVA_CLASS:
                        renameJavaClassTest(fqNameUnsafe.asString(), strings[2]);
                        break;
                    case JAVA_METHOD:
                        renameJavaMethodTest(fqNameUnsafe.asString(), strings[2], strings[3]);
                        break;
                    case KOTLIN_CLASS:
                        renameKotlinClassTest(fqNameUnsafe.toSafe(), strings[2]);
                        break;
                    case KOTLIN_FUNCTION:
                        renameKotlinFunctionTest(fqNameUnsafe.parent().toSafe(), fqNameUnsafe.shortName().asString(), strings[2]);
                        break;
                    case KOTLIN_PROPERTY:
                        renameKotlinPropertyTest(fqNameUnsafe.parent().toSafe(), fqNameUnsafe.shortName().asString(), strings[2]);
                        break;
                }

                if (hintDirective != null) {
                    Assert.fail(String.format("Hint \"%s\" was expected", hintDirective));
                }
            }
            catch (CommonRefactoringUtil.RefactoringErrorHintException hintException) {
                String hintExceptionUnquoted = StringUtil.unquoteString(hintException.getMessage());
                if (hintDirective != null) {
                    Assert.assertEquals(hintDirective, hintExceptionUnquoted);
                }
                else {
                    Assert.fail(String.format("Unexpected hint: // HINT: %s", hintExceptionUnquoted));
                }
            }
        }
        catch (Exception e) {
            throw ExceptionUtils.rethrow(e);
        }
    }

    private void renameJavaClassTest(@NonNls final String qClassName, @NonNls final String newName) throws Exception {
        doTest(new MultiFileTestCase.PerformAction() {
            @Override
            public void performAction(VirtualFile rootDir, VirtualFile rootAfter) throws Exception {
                PsiClass aClass = myJavaFacade.findClass(qClassName, GlobalSearchScope.allScope(getProject()));
                assertNotNull("Class " + qClassName + " not found", aClass);

                PsiElement substitution = RenamePsiElementProcessor.forElement(aClass).substituteElementToRename(aClass, null);

                new RenameProcessor(myProject, substitution, newName, true, true).run();

                PsiDocumentManager.getInstance(myProject).commitAllDocuments();
                FileDocumentManager.getInstance().saveAllDocuments();
            }
        });
    }

    private void renameJavaMethodTest(final String className, final String methodSignature, final String newName) throws Exception {
        doTest(new MultiFileTestCase.PerformAction() {
            @Override
            public void performAction(VirtualFile rootDir, VirtualFile rootAfter) throws Exception {
                JavaPsiFacade manager = getJavaFacade();

                PsiClass aClass = manager.findClass(className, GlobalSearchScope.moduleScope(myModule));
                assertNotNull("Class " + className + " not found", aClass);

                PsiMethod methodBySignature = aClass.findMethodBySignature(manager.getElementFactory().createMethodFromText(methodSignature + "{}", null), false);
                assertNotNull("Method with signature '" + methodSignature + "' wasn't found in class " + className, methodBySignature);

                new RenameProcessor(myProject, methodBySignature, newName, false, false).run();

                PsiDocumentManager.getInstance(myProject).commitAllDocuments();
                FileDocumentManager.getInstance().saveAllDocuments();
            }
        });
    }

    private void renameKotlinFunctionTest(final FqName qClassName, final String oldMethodName, String newMethodName) throws Exception {
        doTestWithKotlinRename(new Function<PsiFile, PsiElement>() {
            @Override
            public PsiElement fun(PsiFile file) {
                BindingContext bindingContext = AnalyzerFacadeWithCache.analyzeFileWithCache((JetFile) file)
                        .getBindingContext();
                ClassDescriptor classDescriptor = bindingContext.get(BindingContext.FQNAME_TO_CLASS_DESCRIPTOR, qClassName);

                assertNotNull(classDescriptor);
                JetScope scope = classDescriptor.getMemberScope(Collections.<TypeProjection>emptyList());
                FunctionDescriptor methodDescriptor = scope.getFunctions(Name.identifier(oldMethodName)).iterator().next();
                return BindingContextUtils.callableDescriptorToDeclaration(bindingContext, methodDescriptor);
            }
        }, newMethodName);
    }

    private void renameKotlinPropertyTest(final FqName qClassName, final String oldPropertyName, String newPropertyName) throws Exception {
        doTestWithKotlinRename(new Function<PsiFile, PsiElement>() {
            @Override
            public PsiElement fun(PsiFile file) {
                BindingContext bindingContext = AnalyzerFacadeWithCache.analyzeFileWithCache((JetFile) file).getBindingContext();
                ClassDescriptor classDescriptor = bindingContext.get(BindingContext.FQNAME_TO_CLASS_DESCRIPTOR, qClassName);
                assertNotNull(classDescriptor);

                JetScope scope = classDescriptor.getMemberScope(Collections.<TypeProjection>emptyList());
                VariableDescriptor propertyName = scope.getProperties(Name.identifier(oldPropertyName)).iterator().next();
                return BindingContextUtils.descriptorToDeclaration(bindingContext, propertyName);
            }
        }, newPropertyName);
    }

    private void renameKotlinClassTest(@NonNls final FqName qClassName, @NonNls String newName) throws Exception {
        doTestWithKotlinRename(new Function<PsiFile, PsiElement>() {
            @Override
            public PsiElement fun(PsiFile file) {
                BindingContext bindingContext = AnalyzerFacadeWithCache.analyzeFileWithCache((JetFile) file)
                        .getBindingContext();
                ClassDescriptor classDescriptor = bindingContext.get(BindingContext.FQNAME_TO_CLASS_DESCRIPTOR, qClassName);

                assertNotNull(classDescriptor);

                return BindingContextUtils.classDescriptorToDeclaration(bindingContext, classDescriptor);
            }
        }, newName);
    }

    private void doTestWithKotlinRename(@NonNls final Function<PsiFile, PsiElement> elementToRenameCallback, final @NonNls String newName) throws Exception {
        doTest(new MultiFileTestCase.PerformAction() {
            @Override
            public void performAction(VirtualFile rootDir, VirtualFile rootAfter) throws Exception {
                VirtualFile child = rootDir.findChild(getTestDirName(false) + ".kt");
                assertNotNull(child);

                Document document = FileDocumentManager.getInstance().getDocument(child);
                assertNotNull(document);

                PsiFile file = PsiDocumentManager.getInstance(getProject()).getPsiFile(document);
                assertTrue(file instanceof JetFile);

                PsiElement psiElement = elementToRenameCallback.fun(file);
                assertNotNull(psiElement);

                PsiElement substitution = RenamePsiElementProcessor.forElement(psiElement).substituteElementToRename(psiElement, null);
                assert substitution != null;

                new RenameProcessor(myProject, substitution, newName, true, true).run();

                PsiDocumentManager.getInstance(myProject).commitAllDocuments();
                FileDocumentManager.getInstance().saveAllDocuments();
                VirtualFileManager.getInstance().syncRefresh();
            }
        });
    }

    protected String getTestDirName(boolean lowercaseFirstLetter) {
        String testName = getTestName(lowercaseFirstLetter);
        return testName.substring(0, testName.indexOf('_'));
    }

    @Override
    protected void doTest(PerformAction performAction) throws Exception {
        super.doTest(performAction, getTestDirName(true));
    }

    @Override
    protected String getTestRoot() {
        return "/refactoring/rename/";
    }

    @Override
    protected String getTestDataPath() {
        return PluginTestCaseBase.getTestDataPathBase();
    }
}
