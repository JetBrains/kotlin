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

package org.jetbrains.jet.plugin.codeInsight;

import com.intellij.codeInsight.generation.OverrideImplementUtil;
import com.intellij.codeInsight.generation.PsiMethodMember;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PostprocessReformattingAspect;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.util.SmartList;
import junit.framework.Assert;
import kotlin.Function1;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.OverrideResolver;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.plugin.JetLightCodeInsightFixtureTestCase;
import org.jetbrains.jet.plugin.JetLightProjectDescriptor;
import org.jetbrains.jet.plugin.caches.resolve.ResolvePackage;
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache;
import org.jetbrains.jet.plugin.project.ResolveSessionForBodies;

import java.util.*;

public abstract class AbstractOverrideImplementTest extends JetLightCodeInsightFixtureTestCase {
    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return JetLightProjectDescriptor.INSTANCE;
    }

    protected void doImplementFileTest() {
        doFileTest(new ImplementMethodsHandler());
    }

    protected void doOverrideFileTest() {
        doFileTest(new OverrideMethodsHandler());
    }

    protected void doMultiImplementFileTest() {
        doMultiFileTest(new ImplementMethodsHandler());
    }

    protected void doMultiOverrideFileTest() {
        doMultiFileTest(new OverrideMethodsHandler());
    }

    protected void doImplementDirectoryTest() {
        doDirectoryTest(new ImplementMethodsHandler());
    }

    protected void doOverrideDirectoryTest(@Nullable String memberToImplement) {
        doDirectoryTest(new OverrideMethodsHandler(), memberToImplement);
    }

    protected void doImplementJavaDirectoryTest(String className, String methodName) {
        myFixture.copyDirectoryToProject(getTestName(true), "");
        myFixture.configureFromTempProjectFile("foo/JavaClass.java");

        Project project = myFixture.getProject();

        PsiClass aClass = JavaPsiFacade.getInstance(project).findClass(className, GlobalSearchScope.allScope(project));
        Assert.assertNotNull("Can't find class: " + className, aClass);

        PsiMethod method = aClass.findMethodsByName(methodName, false)[0];
        Assert.assertNotNull(String.format("Can't find method '%s' in class %s", methodName, className), method);

        generateImplementation(method);

        myFixture.checkResultByFile(getTestName(true) + "/foo/JavaClass.java.after");
    }

    private void doFileTest(OverrideImplementMethodsHandler handler) {
        myFixture.configureByFile(getTestName(true) + ".kt");
        doOverrideImplement(handler, null);
        myFixture.checkResultByFile(getTestName(true) + ".kt.after");
    }

    private void doMultiFileTest(OverrideImplementMethodsHandler handler) {
        myFixture.configureByFile(getTestName(true) + ".kt");
        doMultiOverrideImplement(handler);
        myFixture.checkResultByFile(getTestName(true) + ".kt.after");
    }

    protected void doDirectoryTest(OverrideImplementMethodsHandler handler) {
        doDirectoryTest(handler, null);
    }

    private void doDirectoryTest(OverrideImplementMethodsHandler handler, @Nullable String memberToOverride) {
        myFixture.copyDirectoryToProject(getTestName(true), "");
        myFixture.configureFromTempProjectFile("foo/Impl.kt");
        doOverrideImplement(handler, memberToOverride);
        myFixture.checkResultByFile(getTestName(true) + "/foo/Impl.kt.after");
    }

    private void doOverrideImplement(OverrideImplementMethodsHandler handler, @Nullable String memberToOverride) {
        PsiElement elementAtCaret = myFixture.getFile().findElementAt(myFixture.getEditor().getCaretModel().getOffset());
        final JetClassOrObject classOrObject = PsiTreeUtil.getParentOfType(elementAtCaret, JetClassOrObject.class);
        assertNotNull("Caret should be inside class or object", classOrObject);

        final JetFile jetFile = classOrObject.getContainingJetFile();
        final ResolveSessionForBodies resolveSession = ResolvePackage.getLazyResolveSession(jetFile);
        Set<CallableMemberDescriptor> descriptors =
                handler.collectMethodsToGenerate(classOrObject, resolveSession.resolveToElement(classOrObject));

        final CallableMemberDescriptor singleToOverride;
        if (memberToOverride == null) {
            // Filter out fake overrides of members of Any (equals, hashCode, toString)
            List<CallableMemberDescriptor> filtered = KotlinPackage.filter(descriptors, new Function1<CallableMemberDescriptor, Boolean>() {
                @Override
                public Boolean invoke(CallableMemberDescriptor descriptor) {
                    ClassDescriptor any = KotlinBuiltIns.getInstance().getAny();
                    for (CallableMemberDescriptor overridden : OverrideResolver.getOverriddenDeclarations(descriptor)) {
                        if (overridden.getContainingDeclaration().equals(any)) {
                            return false;
                        }
                    }
                    return true;
                }
            });
            assertEquals("Invalid number of available descriptors for override", 1, filtered.size());
            singleToOverride = filtered.iterator().next();
        }
        else {
            CallableMemberDescriptor candidateToOverride = null;
            for (CallableMemberDescriptor callable : descriptors) {
                if (callable.getName().asString().equals(memberToOverride)) {
                    if (candidateToOverride != null) {
                        throw new IllegalStateException("more then one descriptor with name " + memberToOverride);
                    }
                    candidateToOverride = callable;
                }
            }
            if (candidateToOverride == null) {
                throw new IllegalStateException("no descriptors to override with name " + memberToOverride + " found");
            }
            singleToOverride = candidateToOverride;
        }

        new WriteCommandAction(myFixture.getProject(), myFixture.getFile()) {
            @Override
            protected void run(Result result) throws Throwable {
                OverrideImplementMethodsHandler.generateMethods(myFixture.getEditor(), classOrObject,
                        OverrideImplementMethodsHandler.membersFromDescriptors(jetFile, Collections.singletonList(singleToOverride))
                );
            }
        }.execute();
    }

    private void doMultiOverrideImplement(OverrideImplementMethodsHandler handler) {
        PsiElement elementAtCaret = myFixture.getFile().findElementAt(myFixture.getEditor().getCaretModel().getOffset());
        final JetClassOrObject classOrObject = PsiTreeUtil.getParentOfType(elementAtCaret, JetClassOrObject.class);
        assertNotNull("Caret should be inside class or object", classOrObject);

        final JetFile jetFile = classOrObject.getContainingJetFile();
        BindingContext bindingContext = AnalyzerFacadeWithCache.getContextForElement(classOrObject);
        Set<CallableMemberDescriptor> descriptors = handler.collectMethodsToGenerate(classOrObject, bindingContext);

        final ArrayList<CallableMemberDescriptor> descriptorsList = new ArrayList<CallableMemberDescriptor>(descriptors);
        Collections.sort(descriptorsList, new Comparator<CallableMemberDescriptor>() {
            @Override
            public int compare(@NotNull CallableMemberDescriptor desc1, @NotNull CallableMemberDescriptor desc2) {
                return desc1.getName().compareTo(desc2.getName());
            }
        });

        new WriteCommandAction(myFixture.getProject(), myFixture.getFile()) {
            @Override
            protected void run(Result result) throws Throwable {
                OverrideImplementMethodsHandler.generateMethods(
                        myFixture.getEditor(), classOrObject,
                        OverrideImplementMethodsHandler.membersFromDescriptors(jetFile, descriptorsList));
            }
        }.execute();
    }

    private void generateImplementation(@NotNull final PsiMethod method) {
        WriteCommandAction.runWriteCommandAction(getProject(), new Runnable() {
            @Override
            public void run() {
                PsiClass aClass = ((PsiClassOwner) myFixture.getFile()).getClasses()[0];

                PsiMethodMember methodMember = new PsiMethodMember(method, PsiSubstitutor.EMPTY);

                OverrideImplementUtil.overrideOrImplementMethodsInRightPlace(
                        myFixture.getEditor(), aClass, new SmartList<PsiMethodMember>(methodMember), false);

                PostprocessReformattingAspect.getInstance(myFixture.getProject()).doPostponedFormatting();
            }
        });
    }
}