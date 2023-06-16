/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.asJava;

import com.google.common.collect.Sets;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import kotlin.collections.ArraysKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.cli.jvm.config.JvmContentRootsKt;
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.name.SpecialNames;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.jetbrains.kotlin.asJava.KotlinLightClassStructureTest.ClassProperty.*;

@SuppressWarnings("JUnitTestClassNamingConvention")
public abstract class KotlinLightClassStructureTest extends KotlinAsJavaTestBase {

    public static class Declared extends KotlinLightClassStructureTest {

        @Override
        protected List<File> getKotlinSourceRoots() {
            return Collections.singletonList(
                    new File("compiler/testData/asJava/lightClasses/lightClassStructure/Declared.kt")
            );
        }

        public void testNoModifiers() {
            checkModifiers("test.NoModifiers", PUBLIC, FINAL);
        }

        public void testTopLevelVisibilities() {
            checkModifiers("test.Public", PUBLIC, FINAL);
            checkModifiers("test.Private", PACKAGE_LOCAL, FINAL);
            checkModifiers("test.Internal", PUBLIC, FINAL);
        }

        public void testNestedVisibilities() {
            checkModifiers("test.Outer.Public", PUBLIC, STATIC, FINAL, NESTED);
            checkModifiers("test.Outer.Protected", PROTECTED, STATIC, FINAL, NESTED);
            checkModifiers("test.Outer.Internal", PUBLIC, STATIC, FINAL, NESTED);
            checkModifiers("test.Outer.Private", PRIVATE, STATIC, FINAL, NESTED);

            checkModifiers("test.Outer.Inner", PUBLIC, FINAL, NESTED);
        }
        public void testModalities() {
            checkModifiers("test.Abstract", PUBLIC, ABSTRACT);
            checkModifiers("test.Open", PUBLIC);
            checkModifiers("test.Final", PUBLIC, FINAL);
        }

        public void testAnnotation() {
            checkModifiers("test.Annotation", PUBLIC, ANNOTATION, ABSTRACT, INTERFACE);
        }

        public void testEnum() {
            checkModifiers("test.Enum", PUBLIC, FINAL, ENUM);
        }

        public void testTrait() {
            checkModifiers("test.Trait", PUBLIC, ABSTRACT, INTERFACE);
        }

        public void testDeprecation() {
            checkModifiers("test.DeprecatedClass", PUBLIC, FINAL, DEPRECATED);
            checkModifiers("test.DeprecatedFQN", PUBLIC, FINAL, DEPRECATED);
            checkModifiers("test.DeprecatedFQNSpaces", PUBLIC, FINAL, DEPRECATED);
            checkModifiers("test.DeprecatedWithBrackets", PUBLIC, FINAL, DEPRECATED);
            checkModifiers("test.DeprecatedWithBracketsFQN", PUBLIC, FINAL, DEPRECATED);
            checkModifiers("test.DeprecatedWithBracketsFQNSpaces", PUBLIC, FINAL, DEPRECATED);
        }

        public void testGenericity() {
            checkModifiers("test.Generic1", PUBLIC, FINAL, GENERIC);
            checkModifiers("test.Generic2", PUBLIC, FINAL, GENERIC);
        }
    }

    public static class DeclaredWithGenerics extends KotlinLightClassStructureTest {

        @Override
        protected List<File> getKotlinSourceRoots() {
            return Collections.singletonList(
                    new File("compiler/testData/asJava/lightClasses/lightClassStructure/DeclaredWithGenerics.kt")
            );
        }

        public void testGeneric1() throws Exception {
            checkClassGenericParameter("test.Generic1", 0, "T");
        }

        public void testGeneric1WithBounds() throws Exception {
            checkClassGenericParameter("test.Generic1WithBounds", 0, "T", "test.Bound1");
        }

        public void testGeneric2() throws Exception {
            checkClassGenericParameter("test.Generic2", 0, "A");
            checkClassGenericParameter("test.Generic2", 1, "B");
        }

        public void testGeneric2WithBounds() throws Exception {
            checkClassGenericParameter("test.Generic2WithBounds", 0, "A", "test.Bound1", "test.Bound2");
            checkClassGenericParameter("test.Generic2WithBounds", 1, "B", "test.Generic1<A>");
        }
    }

    public static class PlatformStaticMethodsWithGenerics extends KotlinLightClassStructureTest {
        @Override
        protected List<File> getKotlinSourceRoots() {
            return Collections.singletonList(
                    new File("compiler/testData/asJava/lightClasses/lightClassStructure/PlatformStaticMethodsGenerics.kt")
            );
        }

        public void testInClassObjectSynthetic() throws Exception {
            checkMethodGenericParameter("test.PlatformStaticClass", "inClassObject", 0, "T");
        }

        public void testInClassObjectActual() throws Exception {
            checkMethodGenericParameter("test.PlatformStaticClass." + SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT.asString(), "inClassObject", 0, "T");
        }

        public void testInClass() throws Exception {
            checkMethodGenericParameter("test.PlatformStaticClass", "inClass", 0, "T");
        }

        @Override
        protected void extraConfiguration(@NotNull CompilerConfiguration configuration) {
            JvmContentRootsKt.addJvmClasspathRoot(configuration, ForTestCompileRuntime.runtimeJarForTests());
        }
    }

    public static class Package extends KotlinLightClassStructureTest {

        @Override
        protected List<File> getKotlinSourceRoots() {
            return Collections.singletonList(
                    new File("compiler/testData/asJava/lightClasses/lightClassStructure/Package.kt")
            );
        }

        public void testPackage() throws Exception {
            checkModifiers("test.PackageKt", PUBLIC, FINAL);
        }
    }

    public static class CodeWithErrors extends KotlinLightClassStructureTest {
        @Override
        protected List<File> getKotlinSourceRoots() {
            return Collections.singletonList(new File("compiler/testData/asJava/lightClasses/lightClassStructure/CodeWithErrors.kt"));
        }

        public void testClassWithErrors() {
            assertTrue(findMethodsOfClass("test.C").length == 2);
        }

        public void testFileFacadeWithErrors() {
            assertTrue(findMethodsOfClass("test.CodeWithErrorsKt").length == 1);
        }

        private PsiMethod[] findMethodsOfClass(String qualifiedName) {
            return findClass(qualifiedName).getMethods();
        }
    }

    @NotNull
    protected PsiClass findClass(String qualifiedName) {
        PsiClass psiClass = finder.findClass(qualifiedName, GlobalSearchScope.allScope(getProject()));
        assertNotNull(psiClass);

        assertEquals("Wrong fqn", qualifiedName, psiClass.getQualifiedName());

        return psiClass;
    }

    protected static void checkModifiers(PsiClass psiClass, ClassProperty... properties) {
        Set<ClassProperty> modifiersSet = Sets.newHashSet(properties);
        for (ClassProperty property : values()) {
            boolean present = property.present(psiClass);
            if (modifiersSet.contains(property)) {
                assertTrue("Property " + property + " not present on " + psiClass, present);
            }
            else {
                assertFalse("Property " + property + " must not be present on " + psiClass, present);
            }
        }
    }

    protected void checkModifiers(String classFqName, ClassProperty... properties) {
        checkModifiers(findClass(classFqName), properties);
    }

    protected void checkClassGenericParameter(String classFqName, int index, String name, String... bounds) {
        checkGenericParameter(findClass(classFqName).getTypeParameters()[index], index, name, bounds);
    }

    protected void checkMethodGenericParameter(String classFqName, String methodName, int index, String name, String... bounds) {
        PsiClass aClass = findClass(classFqName);

        PsiMethod method = null;
        for (PsiMethod psiMethod : aClass.getMethods()) {
            if (methodName.equals(psiMethod.getName())) {
                assertNull(String.format("Several methods with name '%s' found in class '%s'", methodName, classFqName), method);
                method = psiMethod;
            }
        }

        assertNotNull(String.format("Methods name '%s' wasn't found in class '%s'", methodName, classFqName), method);

        checkGenericParameter(method.getTypeParameters()[index], index, name, bounds);
    }

    protected static void checkGenericParameter(PsiTypeParameter typeParameter, int index, String name, String[] bounds) {
        assertEquals(name, typeParameter.getName());
        assertEquals(index, typeParameter.getIndex());
        Set<String> expectedBounds = Sets.newHashSet(bounds);
        Set<String> actualBounds = Sets.newHashSet(ArraysKt.map(typeParameter.getExtendsListTypes(), PsiType::getCanonicalText));

        assertEquals(expectedBounds, actualBounds);
    }

    enum ClassProperty {
        PUBLIC(PsiModifier.PUBLIC), 
        PROTECTED(PsiModifier.PROTECTED),
        PACKAGE_LOCAL(PsiModifier.PACKAGE_LOCAL),
        PRIVATE(PsiModifier.PRIVATE),
        STATIC(PsiModifier.STATIC),
        ABSTRACT(PsiModifier.ABSTRACT),
        FINAL(PsiModifier.FINAL),
        NATIVE(PsiModifier.NATIVE),
        SYNCHRONIZED(PsiModifier.SYNCHRONIZED),
        STRICTFP(PsiModifier.STRICTFP),
        TRANSIENT(PsiModifier.TRANSIENT),
        VOLATILE(PsiModifier.VOLATILE),
        DEFAULT(PsiModifier.DEFAULT),
        INTERFACE {
            @Override
            public boolean present(@NotNull PsiClass psiClass) {
                return psiClass.isInterface();
            }
        },
        ENUM {
            @Override
            public boolean present(@NotNull PsiClass psiClass) {
                return psiClass.isEnum();
            }
        },
        ANNOTATION {
            @Override
            public boolean present(@NotNull PsiClass psiClass) {
                return psiClass.isAnnotationType();
            }
        },
        DEPRECATED {
            @Override
            public boolean present(@NotNull PsiClass psiClass) {
                return psiClass.isDeprecated();
            }
        },
        NESTED {
            @Override
            public boolean present(@NotNull PsiClass psiClass) {
                return psiClass.getContainingClass() != null;
            }
        },
        GENERIC {
            @Override
            public boolean present(@NotNull PsiClass psiClass) {
                return psiClass.hasTypeParameters();
            }
        };

        private final String modifier;

        private ClassProperty(@Nullable String modifier) {
            this.modifier = modifier;
        }

        private ClassProperty() {
            this(null);
        }

        public boolean present(@NotNull PsiClass psiClass) {
            assert modifier != null : "No modifier specified for " + this + ". Override this method.";
            return psiClass.hasModifierProperty(modifier);
        }
    }

}
