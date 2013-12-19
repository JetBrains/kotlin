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

package org.jetbrains.jet.plugin.ktSignature;

import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.di.InjectorForJavaDescriptorResolver;
import org.jetbrains.jet.di.InjectorForJavaDescriptorResolverUtil;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.plugin.JetFileType;
import org.jetbrains.jet.plugin.JetLightProjectDescriptor;
import org.jetbrains.jet.plugin.caches.resolve.KotlinCacheManager;
import org.jetbrains.jet.plugin.project.TargetPlatform;

import static org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule.IGNORE_KOTLIN_SOURCES;

public class SignatureMarkerProviderTest extends LightCodeInsightFixtureTestCase {

    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return JetLightProjectDescriptor.INSTANCE;
    }

    public void testReResolveJavaClass() {
        Project project;
        project = myFixture.getProject();


        myFixture.configureByText(JetFileType.INSTANCE, "val t: Thread? = null");

        PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass("java.lang.Thread", GlobalSearchScope.allScope(project));
        BindingContext context = KotlinCacheManager.getInstance(project).getDeclarationsFromProject(TargetPlatform.JVM).getBindingContext();
        ClassDescriptor preResolvedClass = context.get(BindingContext.CLASS, psiClass);

        InjectorForJavaDescriptorResolver injector =
                InjectorForJavaDescriptorResolverUtil.create(project, KotlinSignatureInJavaMarkerProvider.createDelegatingTrace(project));
        ClassDescriptor reResolvedClass = injector.getJavaDescriptorResolver()
                .resolveClass(new FqName("java.lang.Thread"), IGNORE_KOTLIN_SOURCES);

        assertSame(preResolvedClass, reResolvedClass);
    }
}
