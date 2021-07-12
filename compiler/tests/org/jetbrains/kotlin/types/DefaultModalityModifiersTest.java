/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.types;

import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.analyzer.AnalysisResult;
import org.jetbrains.kotlin.analyzer.common.CommonPlatformAnalyzerServices;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl;
import org.jetbrains.kotlin.container.DslKt;
import org.jetbrains.kotlin.container.StorageComponentContainer;
import org.jetbrains.kotlin.context.ContextKt;
import org.jetbrains.kotlin.context.ModuleContext;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.kotlin.incremental.components.NoLookupLocation;
import org.jetbrains.kotlin.platform.CommonPlatforms;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.*;
import org.jetbrains.kotlin.resolve.calls.components.InferenceSession;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfoFactory;
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil;
import org.jetbrains.kotlin.resolve.lazy.ResolveSession;
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory;
import org.jetbrains.kotlin.resolve.scopes.*;
import org.jetbrains.kotlin.resolve.scopes.utils.ScopeUtilsKt;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.kotlin.test.DummyTraces;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.KotlinTestWithEnvironment;
import org.jetbrains.kotlin.tests.di.ContainerForTests;
import org.jetbrains.kotlin.tests.di.InjectionKt;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.frontend.di.InjectionKt.createContainerForLazyResolve;

public class DefaultModalityModifiersTest extends KotlinTestWithEnvironment {
    private final DefaultModalityModifiersTestCase tc = new DefaultModalityModifiersTestCase();

    @Override
    protected KotlinCoreEnvironment createEnvironment() {
        return createEnvironmentWithMockJdk(ConfigurationKind.ALL);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        tc.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        tc.tearDown();
        super.tearDown();
    }

    public class DefaultModalityModifiersTestCase {
        private final ModuleDescriptorImpl root = KotlinTestUtils.createEmptyModule("<test_root>");
        private DescriptorResolver descriptorResolver;
        private FunctionDescriptorResolver functionDescriptorResolver;
        private LexicalScope scope;

        public void setUp() throws Exception {
            ContainerForTests containerForTests = InjectionKt.createContainerForTests(getProject(), root);
            descriptorResolver = containerForTests.getDescriptorResolver();
            functionDescriptorResolver = containerForTests.getFunctionDescriptorResolver();
            scope = createScope(root.getBuiltIns().getBuiltInsPackageScope());
        }

        public void tearDown() throws Exception {
            scope = null;
            descriptorResolver = null;
        }

        @NotNull
        private LexicalScope createScope(@NotNull MemberScope libraryScope) {
            KtFile file =
                    KtPsiFactoryKt.KtPsiFactory(getProject()).createFile("abstract class C { abstract fun foo(); abstract val a: Int }");
            KtDeclaration aClass = file.getDeclarations().get(0);
            assert aClass instanceof KtClass;
            AnalysisResult bindingContext = JvmResolveUtil.analyzeAndCheckForErrors(file, getEnvironment());
            DeclarationDescriptor classDescriptor =
                    bindingContext.getBindingContext().get(BindingContext.DECLARATION_TO_DESCRIPTOR, aClass);
            return new LexicalScopeImpl(
                    ScopeUtilsKt.memberScopeAsImportingScope(libraryScope), root, false, null, Collections.emptyList(),
                    LexicalScopeKind.SYNTHETIC, LocalRedeclarationChecker.DO_NOTHING.INSTANCE,
                    handler -> {
                        handler.addClassifierDescriptor((ClassifierDescriptor) classDescriptor);
                        return Unit.INSTANCE;
                    }
            );
        }

        private ClassDescriptorWithResolutionScopes createClassDescriptor(ClassKind kind, KtClass aClass) {
            ModuleContext moduleContext = ContextKt.ModuleContext(root, getProject(), "DefaultModalityModifiersTest");
            Collection<KtFile> files = Collections.singleton(aClass.getContainingKtFile());

            StorageComponentContainer container = createContainerForLazyResolve(
                    moduleContext,
                    new FileBasedDeclarationProviderFactory(moduleContext.getStorageManager(), files),
                    new BindingTraceContext(),
                    CommonPlatforms.INSTANCE.getDefaultCommonPlatform(),
                    CommonPlatformAnalyzerServices.INSTANCE,
                    CompilerEnvironment.INSTANCE,
                    LanguageVersionSettingsImpl.DEFAULT
            );

            ResolveSession resolveSession = DslKt.getService(container, ResolveSession.class);

            return (ClassDescriptorWithResolutionScopes) resolveSession.getClassDescriptor(aClass, NoLookupLocation.FROM_TEST);
        }

        private void testClassModality(String classDeclaration, ClassKind kind, Modality expectedModality) {
            KtClass aClass = KtPsiFactoryKt.KtPsiFactory(getProject()).createClass(classDeclaration);
            ClassDescriptorWithResolutionScopes classDescriptor = createClassDescriptor(kind, aClass);

            assertEquals(expectedModality, classDescriptor.getModality());
        }


        private void testFunctionModality(String classWithFunction, ClassKind kind, Modality expectedFunctionModality) {
            KtClass aClass = KtPsiFactoryKt.KtPsiFactory(getProject()).createClass(classWithFunction);
            ClassDescriptorWithResolutionScopes classDescriptor = createClassDescriptor(kind, aClass);

            List<KtDeclaration> declarations = aClass.getDeclarations();
            KtNamedFunction function = (KtNamedFunction) declarations.get(0);
            SimpleFunctionDescriptor functionDescriptor =
                    functionDescriptorResolver.resolveFunctionDescriptor(classDescriptor, scope, function,
                                                                         DummyTraces.DUMMY_TRACE, DataFlowInfoFactory.EMPTY, null);

            assertEquals(expectedFunctionModality, functionDescriptor.getModality());
        }

        private void testPropertyModality(String classWithProperty, ClassKind kind, Modality expectedPropertyModality) {
            KtClass aClass = KtPsiFactoryKt.KtPsiFactory(getProject()).createClass(classWithProperty);
            ClassDescriptorWithResolutionScopes classDescriptor = createClassDescriptor(kind, aClass);

            List<KtDeclaration> declarations = aClass.getDeclarations();
            KtProperty property = (KtProperty) declarations.get(0);
            PropertyDescriptor propertyDescriptor = descriptorResolver.resolvePropertyDescriptor(
                    classDescriptor, scope, scope, property,
                    DummyTraces.DUMMY_TRACE, DataFlowInfoFactory.EMPTY,
                    InferenceSession.Companion.getDefault()
            );

            assertEquals(expectedPropertyModality, propertyDescriptor.getModality());
        }


        private void testPropertyAccessorModality(String classWithPropertyWithAccessor, ClassKind kind, Modality expectedPropertyAccessorModality, boolean isGetter) {
            KtClass aClass = KtPsiFactoryKt.KtPsiFactory(getProject()).createClass(classWithPropertyWithAccessor);
            ClassDescriptorWithResolutionScopes classDescriptor = createClassDescriptor(kind, aClass);

            List<KtDeclaration> declarations = aClass.getDeclarations();
            KtProperty property = (KtProperty) declarations.get(0);
            PropertyDescriptor propertyDescriptor = descriptorResolver.resolvePropertyDescriptor(
                    classDescriptor, scope, scope, property,
                    DummyTraces.DUMMY_TRACE, DataFlowInfoFactory.EMPTY,
                    InferenceSession.Companion.getDefault()
            );
            PropertyAccessorDescriptor propertyAccessor = isGetter
                                                          ? propertyDescriptor.getGetter()
                                                          : propertyDescriptor.getSetter();
            assert propertyAccessor != null;
            assertEquals(expectedPropertyAccessorModality, propertyAccessor.getModality());
        }

        public void testClassModality(String classDeclaration, Modality expectedModality) {
            testClassModality(classDeclaration, ClassKind.CLASS, expectedModality);
        }

        public void testTraitModality(String classDeclaration, Modality expectedModality) {
            testClassModality(classDeclaration, ClassKind.INTERFACE, expectedModality);
        }

        public void testEnumModality(String classDeclaration, Modality expectedModality) {
            testClassModality(classDeclaration, ClassKind.ENUM_CLASS, expectedModality);
        }

        public void testFunctionModalityInClass(String classWithFunction, Modality expectedModality) {
            testFunctionModality(classWithFunction, ClassKind.CLASS, expectedModality);
        }

        public void testFunctionModalityInEnum(String classWithFunction, Modality expectedModality) {
            testFunctionModality(classWithFunction, ClassKind.ENUM_CLASS, expectedModality);
        }

        public void testFunctionModalityInTrait(String classWithFunction, Modality expectedModality) {
            testFunctionModality(classWithFunction, ClassKind.INTERFACE, expectedModality);
        }

        public void testPropertyModalityInClass(String classWithProperty, Modality expectedModality) {
            testPropertyModality(classWithProperty, ClassKind.CLASS, expectedModality);
        }

        public void testPropertyModalityInEnum(String classWithProperty, Modality expectedModality) {
            testPropertyModality(classWithProperty, ClassKind.ENUM_CLASS, expectedModality);
        }

        public void testPropertyModalityInTrait(String classWithProperty, Modality expectedModality) {
            testPropertyModality(classWithProperty, ClassKind.INTERFACE, expectedModality);
        }

        public void testPropertyAccessorModalityInClass(String classWithPropertyWithAccessor, Modality expectedModality) {
            testPropertyAccessorModality(classWithPropertyWithAccessor, ClassKind.CLASS, expectedModality, true);
        }

        public void testPropertyAccessorModalityInTrait(String classWithPropertyWithAccessor, Modality expectedModality) {
            testPropertyAccessorModality(classWithPropertyWithAccessor, ClassKind.INTERFACE, expectedModality, true);
        }

        public void testPropertyAccessorModalityInClass(String classWithPropertyWithAccessor, Modality expectedModality, boolean isGetter) {
            testPropertyAccessorModality(classWithPropertyWithAccessor, ClassKind.CLASS, expectedModality, isGetter);
        }

        public void testPropertyAccessorModalityInTrait(String classWithPropertyWithAccessor, Modality expectedModality, boolean isGetter) {
            testPropertyAccessorModality(classWithPropertyWithAccessor, ClassKind.INTERFACE, expectedModality, isGetter);
        }
    }

    public void testClassModality() {
        tc.testClassModality("class A {}", Modality.FINAL);
        tc.testClassModality("open class A {}", Modality.OPEN);
        tc.testClassModality("abstract class A {}", Modality.ABSTRACT);
        tc.testClassModality("final class A {}", Modality.FINAL);
        tc.testClassModality("open abstract class A {}", Modality.ABSTRACT);

        tc.testEnumModality("enum class A {}", Modality.FINAL);
        tc.testEnumModality("open enum class A {}", Modality.OPEN);
        tc.testEnumModality("abstract enum class A {}", Modality.ABSTRACT);
        tc.testEnumModality("final enum class A {}", Modality.FINAL);

        tc.testTraitModality("interface A {}", Modality.ABSTRACT);
        tc.testTraitModality("open interface A {}", Modality.ABSTRACT);
        tc.testTraitModality("abstract interface A {}", Modality.ABSTRACT);
    }

    public void testFunctionModality() {
        tc.testFunctionModalityInClass("class A { fun foo() {} }", Modality.FINAL);
        tc.testFunctionModalityInClass("class A { open fun foo() {} }", Modality.OPEN);
        tc.testFunctionModalityInClass("class A { final fun foo() {} }", Modality.FINAL);
        tc.testFunctionModalityInClass("open class A { fun foo() {} }", Modality.FINAL);
        tc.testFunctionModalityInClass("open class A { open fun foo() {} }", Modality.OPEN);
        tc.testFunctionModalityInClass("open class A { final fun foo() {} }", Modality.FINAL);
        tc.testFunctionModalityInClass("abstract class A { open fun foo() }", Modality.OPEN);
        tc.testFunctionModalityInClass("abstract class A { abstract fun foo() }", Modality.ABSTRACT);

        tc.testFunctionModalityInEnum("enum class A { ; fun foo() {} }", Modality.FINAL);
        tc.testFunctionModalityInEnum("enum class A { ; final fun foo() {} }", Modality.FINAL);
        tc.testFunctionModalityInEnum("open enum class A { ; open fun foo() {} }", Modality.OPEN);
        tc.testFunctionModalityInEnum("abstract enum class A { ; open fun foo() }", Modality.OPEN);
        tc.testFunctionModalityInEnum("abstract enum class A { ; abstract fun foo() }", Modality.ABSTRACT);

        tc.testFunctionModalityInTrait("interface A { fun foo() }", Modality.ABSTRACT);
        tc.testFunctionModalityInTrait("interface A { abstract fun foo() }", Modality.ABSTRACT);
        tc.testFunctionModalityInTrait("interface A { fun foo() {} }", Modality.OPEN);
        tc.testFunctionModalityInTrait("interface A { open fun foo() {} }", Modality.OPEN);
        tc.testFunctionModalityInTrait("interface A { final fun foo() {} }", Modality.FINAL);
    }

    public void testFunctionModalityWithOverride() {
        tc.testFunctionModalityInClass("class A : C { override fun foo() {} }", Modality.OPEN);
        tc.testFunctionModalityInClass("class A : C { open override fun foo() {} }", Modality.OPEN);
        tc.testFunctionModalityInClass("class A : C { final override fun foo() {} }", Modality.FINAL);

        tc.testFunctionModalityInClass("open class A : C { override fun foo() {} }", Modality.OPEN);
        tc.testFunctionModalityInClass("open class A : C { open override fun foo() {} }", Modality.OPEN);
        tc.testFunctionModalityInClass("open class A : C { final override fun foo() {} }", Modality.FINAL);
        tc.testFunctionModalityInClass("abstract class A : C { open override  fun foo() }", Modality.OPEN);
        tc.testFunctionModalityInClass("abstract class A : C { abstract override fun foo() }", Modality.ABSTRACT);

        tc.testFunctionModalityInEnum("enum class A : C { ; override fun foo() {} }", Modality.OPEN);
        tc.testFunctionModalityInEnum("enum class A : C { ; final override fun foo() {} }", Modality.FINAL);
        tc.testFunctionModalityInEnum("open enum class A : C { ; open override fun foo() {} }", Modality.OPEN);
        tc.testFunctionModalityInEnum("abstract enum class A : C { ; open override fun foo() }", Modality.OPEN);
        tc.testFunctionModalityInEnum("abstract enum class A : C { ; abstract override fun foo() }", Modality.ABSTRACT);

        tc.testFunctionModalityInTrait("interface A : C { override fun foo() }", Modality.ABSTRACT);
        tc.testFunctionModalityInTrait("interface A : C { abstract override fun foo() }", Modality.ABSTRACT);
        tc.testFunctionModalityInTrait("interface A : C { override fun foo() {} }", Modality.OPEN);
        tc.testFunctionModalityInTrait("interface A : C { open override fun foo() {} }", Modality.OPEN);
        tc.testFunctionModalityInTrait("interface A : C { final override fun foo() {} }", Modality.FINAL);
    }

    public void testPropertyModality() {
        tc.testPropertyModalityInClass("class A { val a: Int = 0 }", Modality.FINAL);
        tc.testPropertyModalityInClass("class A { final val a: Int = 0 }", Modality.FINAL);
        tc.testPropertyModalityInClass("open class A { val a: Int = 0 }", Modality.FINAL);
        tc.testPropertyModalityInClass("open class A { final val a: Int = 0 }", Modality.FINAL);
        tc.testPropertyModalityInClass("open class A { open val a: Int = 0 }", Modality.OPEN);
        tc.testPropertyModalityInClass("abstract class A { val a: Int = 0 }", Modality.FINAL);
        tc.testPropertyModalityInClass("abstract class A { open val a: Int = 0 }", Modality.OPEN);
        tc.testPropertyModalityInClass("abstract class A { abstract val a: Int }", Modality.ABSTRACT);

        tc.testPropertyModalityInEnum("enum class A { ; val a: Int = 0 }", Modality.FINAL);
        tc.testPropertyModalityInEnum("enum class A { ; final val a: Int = 0 }", Modality.FINAL);
        tc.testPropertyModalityInEnum("open enum class A { ; val a: Int = 0 }", Modality.FINAL);
        tc.testPropertyModalityInEnum("open enum class A { ; final val a: Int = 0 }", Modality.FINAL);
        tc.testPropertyModalityInEnum("open enum class A { ; open val a: Int = 0 }", Modality.OPEN);
        tc.testPropertyModalityInEnum("abstract enum class A { ; open val a: Int = 0 }", Modality.OPEN);
        tc.testPropertyModalityInEnum("abstract enum class A { ; abstract val a: Int }", Modality.ABSTRACT);

        tc.testPropertyModalityInTrait("interface A { val a: Int }", Modality.ABSTRACT);
        tc.testPropertyModalityInTrait("interface A { open val a: Int }", Modality.ABSTRACT);
        tc.testPropertyModalityInTrait("interface A { abstract val a: Int }", Modality.ABSTRACT);
        tc.testPropertyModalityInTrait("interface A { open abstract val a: Int }", Modality.ABSTRACT);

        tc.testPropertyModalityInTrait("interface A { val a: Int get() = 10 }", Modality.OPEN);
        tc.testPropertyModalityInTrait("interface A { var a: Int get() = 1; set(v: Int) {} }", Modality.OPEN);
        tc.testPropertyModalityInTrait("interface A { val a: Int open get }", Modality.ABSTRACT);
        tc.testPropertyModalityInTrait("interface A { var a: Int open get open set}", Modality.ABSTRACT);
        tc.testPropertyModalityInTrait("interface A { open val a: Int get }", Modality.ABSTRACT);
        tc.testPropertyModalityInTrait("interface A { open val a: Int get() = 1 }", Modality.OPEN);
        tc.testPropertyModalityInTrait("interface A { open val a: Int final get() = 1 }", Modality.OPEN);
    }

    public void testPropertyModalityWithOverride() {
        tc.testPropertyModalityInClass("class A : C { override val a: Int = 0 }", Modality.OPEN);
        tc.testPropertyModalityInClass("class A : C { final override val a: Int = 0 }", Modality.FINAL);
        tc.testPropertyModalityInClass("open class A : C { override val a: Int = 0 }", Modality.OPEN);
        tc.testPropertyModalityInClass("open class A : C { final override val a: Int = 0 }", Modality.FINAL);
        tc.testPropertyModalityInClass("open class A : C { open override val a: Int = 0 }", Modality.OPEN);
        tc.testPropertyModalityInClass("abstract class A : C { override val a: Int = 0 }", Modality.OPEN);
        tc.testPropertyModalityInClass("abstract class A : C { open override val a: Int = 0 }", Modality.OPEN);
        tc.testPropertyModalityInClass("abstract class A : C { abstract override val a: Int }", Modality.ABSTRACT);

        tc.testPropertyModalityInEnum("enum class A : C { ; override val a: Int = 0 }", Modality.OPEN);
        tc.testPropertyModalityInEnum("enum class A : C { ; final override val a: Int = 0 }", Modality.FINAL);
        tc.testPropertyModalityInEnum("open enum class A : C { ; override val a: Int = 0 }", Modality.OPEN);
        tc.testPropertyModalityInEnum("open enum class A : C { ; final override val a: Int = 0 }", Modality.FINAL);
        tc.testPropertyModalityInEnum("open enum class A : C { ; open override val a: Int = 0 }", Modality.OPEN);
        tc.testPropertyModalityInEnum("abstract enum class A : C { ; open override val a: Int = 0 }", Modality.OPEN);
        tc.testPropertyModalityInEnum("abstract enum class A : C { ; abstract override val a: Int }", Modality.ABSTRACT);

        tc.testPropertyModalityInTrait("interface A : C { override val a: Int }", Modality.ABSTRACT);
        tc.testPropertyModalityInTrait("interface A : C { open override val a: Int }", Modality.ABSTRACT);
        tc.testPropertyModalityInTrait("interface A : C { abstract override val a: Int }", Modality.ABSTRACT);
        tc.testPropertyModalityInTrait("interface A : C { open abstract override val a: Int }", Modality.ABSTRACT);

        tc.testPropertyModalityInTrait("interface A : C { override val a: Int get() = 10 }", Modality.OPEN);
        tc.testPropertyModalityInTrait("interface A : C { override var a: Int get() = 1; set(v: Int) {} }", Modality.OPEN);
        tc.testPropertyModalityInTrait("interface A : C { override val a: Int open get }", Modality.ABSTRACT);
        tc.testPropertyModalityInTrait("interface A : C { override var a: Int open get open set }", Modality.ABSTRACT);
        tc.testPropertyModalityInTrait("interface A : C { open override val a: Int get }", Modality.ABSTRACT);
        tc.testPropertyModalityInTrait("interface A : C { open override val a: Int get() = 1 }", Modality.OPEN);
        tc.testPropertyModalityInTrait("interface A : C { open override val a: Int final get() = 1 }", Modality.OPEN);
    }

    public void testPropertyAccessorModality() {
        tc.testPropertyAccessorModalityInClass("class A { val a: Int = 0 }", Modality.FINAL);
        tc.testPropertyAccessorModalityInClass("class A { val a: Int = 0; get }", Modality.FINAL);
        tc.testPropertyAccessorModalityInClass("class A { val a: Int = 0; final get }", Modality.FINAL);

        tc.testPropertyAccessorModalityInClass("class A { final val a: Int = 0 }", Modality.FINAL);
        tc.testPropertyAccessorModalityInClass("class A { final val a: Int = 0; get }", Modality.FINAL);
        tc.testPropertyAccessorModalityInClass("class A { final val a: Int = 0; final get }", Modality.FINAL);

        tc.testPropertyAccessorModalityInClass("open class A { val a: Int = 0 }", Modality.FINAL);
        tc.testPropertyAccessorModalityInClass("open class A { val a: Int = 0; get }", Modality.FINAL);
        tc.testPropertyAccessorModalityInClass("open class A { val a: Int = 0; final get }", Modality.FINAL);

        tc.testPropertyAccessorModalityInClass("open class A { open val a: Int = 0 }", Modality.OPEN);
        tc.testPropertyAccessorModalityInClass("open class A { open val a: Int = 0; get }", Modality.OPEN);
        tc.testPropertyAccessorModalityInClass("open class A { open val a: Int = 0; open get }", Modality.OPEN);
        tc.testPropertyAccessorModalityInClass("open class A { open val a: Int = 0; final get }", Modality.FINAL);

        tc.testPropertyAccessorModalityInClass("open class A { final val a: Int = 0 }", Modality.FINAL);
        tc.testPropertyAccessorModalityInClass("open class A { final val a: Int = 0; get }", Modality.FINAL);
        tc.testPropertyAccessorModalityInClass("open class A { final val a: Int = 0; final get }", Modality.FINAL);

        tc.testPropertyAccessorModalityInClass("abstract class A { abstract val a: Int }", Modality.ABSTRACT);
        tc.testPropertyAccessorModalityInClass("abstract class A { abstract val a: Int get }", Modality.ABSTRACT);
        tc.testPropertyAccessorModalityInClass("abstract class A { abstract val a: Int open get }", Modality.ABSTRACT);
        tc.testPropertyAccessorModalityInClass("abstract class A { abstract val a: Int abstract get }", Modality.ABSTRACT);
        tc.testPropertyAccessorModalityInClass("abstract class A { val a: Int get() = 10 }", Modality.FINAL);
        tc.testPropertyAccessorModalityInClass("abstract class A { val a: Int open get() = 10 }", Modality.OPEN);
        tc.testPropertyAccessorModalityInClass("abstract class A { val a: Int final get() = 10 }", Modality.FINAL);

        tc.testPropertyAccessorModalityInClass("abstract class A { open abstract val a: Int }", Modality.ABSTRACT);
        tc.testPropertyAccessorModalityInClass("abstract class A { open abstract val a: Int get }", Modality.ABSTRACT);
        tc.testPropertyAccessorModalityInClass("abstract class A { open abstract val a: Int open get }", Modality.ABSTRACT);
        tc.testPropertyAccessorModalityInClass("abstract class A { open abstract val a: Int abstract get }", Modality.ABSTRACT);
        tc.testPropertyAccessorModalityInClass("abstract class A { open val a: Int get() = 10 }", Modality.OPEN);
        tc.testPropertyAccessorModalityInClass("abstract class A { open val a: Int open get() = 10 }", Modality.OPEN);
        tc.testPropertyAccessorModalityInClass("abstract class A { open val a: Int final get() = 10 }", Modality.FINAL);

        tc.testPropertyAccessorModalityInTrait("interface A { val a: Int }", Modality.ABSTRACT);
        tc.testPropertyAccessorModalityInTrait("interface A { val a: Int get }", Modality.ABSTRACT);
        tc.testPropertyAccessorModalityInTrait("interface A { val a: Int get() = 1 }", Modality.OPEN);
        tc.testPropertyAccessorModalityInTrait("interface A { val a: Int abstract get }", Modality.ABSTRACT);
        tc.testPropertyAccessorModalityInTrait("interface A { val a: Int open get }", Modality.ABSTRACT);
        tc.testPropertyAccessorModalityInTrait("interface A { val a: Int open get() = 1 }", Modality.OPEN);
        tc.testPropertyAccessorModalityInTrait("interface A { val a: Int final get }", Modality.FINAL);
        tc.testPropertyAccessorModalityInTrait("interface A { val a: Int final get() = 1 }", Modality.FINAL);

        tc.testPropertyAccessorModalityInTrait("abstract interface A { val a: Int }", Modality.ABSTRACT);
        tc.testPropertyAccessorModalityInTrait("abstract interface A { val a: Int get }", Modality.ABSTRACT);
        tc.testPropertyAccessorModalityInTrait("abstract interface A { val a: Int get() = 1 }", Modality.OPEN);
        tc.testPropertyAccessorModalityInTrait("abstract interface A { val a: Int abstract get }", Modality.ABSTRACT);
        tc.testPropertyAccessorModalityInTrait("abstract interface A { val a: Int open get }", Modality.ABSTRACT);
        tc.testPropertyAccessorModalityInTrait("abstract interface A { val a: Int open get() = 1 }", Modality.OPEN);
        tc.testPropertyAccessorModalityInTrait("abstract interface A { val a: Int final get }", Modality.FINAL);
        tc.testPropertyAccessorModalityInTrait("abstract interface A { val a: Int final get() = 1 }", Modality.FINAL);
    }

    public void testPropertyAccessorModalityWithOverride() {
        tc.testPropertyAccessorModalityInClass("class A : C { override val a: Int = 0 }", Modality.OPEN);
        tc.testPropertyAccessorModalityInClass("class A : C { override val a: Int = 0; get }", Modality.OPEN);
        tc.testPropertyAccessorModalityInClass("class A : C { override val a: Int = 0; final get }", Modality.FINAL);

        tc.testPropertyAccessorModalityInClass("class A : C { final override val a: Int = 0 }", Modality.FINAL);
        tc.testPropertyAccessorModalityInClass("class A : C { final override val a: Int = 0; get }", Modality.FINAL);
        tc.testPropertyAccessorModalityInClass("class A : C { final override val a: Int = 0; final get }", Modality.FINAL);
        tc.testPropertyAccessorModalityInClass("class A : C { final override val a: Int = 0; override get() = 2 }", Modality.OPEN);

        tc.testPropertyAccessorModalityInClass("open class A : C { override val a: Int = 0 }", Modality.OPEN);
        tc.testPropertyAccessorModalityInClass("open class A : C { override val a: Int = 0; get }", Modality.OPEN);
        tc.testPropertyAccessorModalityInClass("open class A : C { override val a: Int = 0; open get }", Modality.OPEN);
        tc.testPropertyAccessorModalityInClass("open class A : C { override val a: Int = 0; final get }", Modality.FINAL);
        tc.testPropertyAccessorModalityInClass("open class A : C { override val a: Int = 0; override get() = 2 }", Modality.OPEN);
        tc.testPropertyAccessorModalityInClass("open class A : C { override val a: Int = 0; final override get() = 2 }", Modality.FINAL);

        tc.testPropertyAccessorModalityInClass("open class A : C { open override val a: Int = 0 }", Modality.OPEN);
        tc.testPropertyAccessorModalityInClass("open class A : C { open override val a: Int = 0; get }", Modality.OPEN);
        tc.testPropertyAccessorModalityInClass("open class A : C { open override val a: Int = 0; open get }", Modality.OPEN);
        tc.testPropertyAccessorModalityInClass("open class A : C { open override val a: Int = 0; final get }", Modality.FINAL);
        tc.testPropertyAccessorModalityInClass("open class A : C { open override val a: Int = 0; override get() = 2 }", Modality.OPEN);
        tc.testPropertyAccessorModalityInClass("open class A : C { open override val a: Int = 0; final override get() = 2 }", Modality.FINAL);

        tc.testPropertyAccessorModalityInClass("open class A : C { final override val a: Int = 0 }", Modality.FINAL);
        tc.testPropertyAccessorModalityInClass("open class A : C { final override val a: Int = 0; get }", Modality.FINAL);
        tc.testPropertyAccessorModalityInClass("open class A : C { final override val a: Int = 0; final get }", Modality.FINAL);
        tc.testPropertyAccessorModalityInClass("open class A : C { final override val a: Int = 0; override get() = 2 }", Modality.OPEN);
        tc.testPropertyAccessorModalityInClass("open class A : C { final override val a: Int = 0; final override get() = 2 }", Modality.FINAL);

        tc.testPropertyAccessorModalityInClass("abstract class A : C { abstract override val a: Int }", Modality.ABSTRACT);
        tc.testPropertyAccessorModalityInClass("abstract class A : C { abstract override val a: Int get }", Modality.ABSTRACT);
        tc.testPropertyAccessorModalityInClass("abstract class A : C { abstract override val a: Int open get }", Modality.ABSTRACT);
        tc.testPropertyAccessorModalityInClass("abstract class A : C { abstract override val a: Int abstract get }", Modality.ABSTRACT);
        tc.testPropertyAccessorModalityInClass("abstract class A : C { override val a: Int override get() = 10 }", Modality.OPEN);
        tc.testPropertyAccessorModalityInClass("abstract class A : C { override val a: Int open override get() = 10 }", Modality.OPEN);
        tc.testPropertyAccessorModalityInClass("abstract class A : C { override val a: Int final override get() = 10 }", Modality.FINAL);

        tc.testPropertyAccessorModalityInClass("abstract class A : C { open abstract override val a: Int }", Modality.ABSTRACT);
        tc.testPropertyAccessorModalityInClass("abstract class A : C { open abstract override val a: Int get }", Modality.ABSTRACT);
        tc.testPropertyAccessorModalityInClass("abstract class A : C { open abstract override val a: Int open get }", Modality.ABSTRACT);
        tc.testPropertyAccessorModalityInClass("abstract class A : C { open abstract override val a: Int abstract get }", Modality.ABSTRACT);
        tc.testPropertyAccessorModalityInClass("abstract class A : C { open override val a: Int override get() = 10 }", Modality.OPEN);
        tc.testPropertyAccessorModalityInClass("abstract class A : C { open override val a: Int open override get() = 10 }", Modality.OPEN);
        tc.testPropertyAccessorModalityInClass("abstract class A : C { open override val a: Int final override  get() = 10 }", Modality.FINAL);

        tc.testPropertyAccessorModalityInTrait("interface A : C { override val a: Int }", Modality.ABSTRACT);
        tc.testPropertyAccessorModalityInTrait("interface A : C { override val a: Int get }", Modality.ABSTRACT);
        tc.testPropertyAccessorModalityInTrait("interface A : C { override val a: Int override get() = 1 }", Modality.OPEN);
        tc.testPropertyAccessorModalityInTrait("interface A : C { override val a: Int abstract get }", Modality.ABSTRACT);
        tc.testPropertyAccessorModalityInTrait("interface A : C { override val a: Int open get }", Modality.ABSTRACT);
        tc.testPropertyAccessorModalityInTrait("interface A : C { override val a: Int open override get() = 1 }", Modality.OPEN);
        tc.testPropertyAccessorModalityInTrait("interface A : C { override val a: Int override get() = 1 }", Modality.OPEN);
        tc.testPropertyAccessorModalityInTrait("interface A : C { override val a: Int final get }", Modality.FINAL);
        tc.testPropertyAccessorModalityInTrait("interface A : C { override val a: Int final override get() = 1 }", Modality.FINAL);
    }
}
