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

package org.jetbrains.jet.asJava;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.config.CommonConfigurationKeys;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.lazy.KotlinTestWithEnvironment;
import org.jetbrains.jet.lang.resolve.lazy.LazyResolveTestUtil;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.jetbrains.jet.asJava.KotlinLightClassTest.ClassProperty.*;

public abstract class KotlinLightClassTest extends KotlinTestWithEnvironment {

    public static class Declared extends KotlinLightClassTest {
        public Declared() {
            super(new File("compiler/testData/asJava/lightClasses/Declared.kt"));
        }

        public void testNoModifiers() {
            checkModifiers("test.NoModifiers", PUBLIC, FINAL);
        }

        public void testTopLevelVisibilities() {
            checkModifiers("test.Public", PUBLIC, FINAL);
            checkModifiers("test.Private", PUBLIC, FINAL);
            checkModifiers("test.Internal", PUBLIC, FINAL);
        }

        public void testNestedVisibilities() {
            checkModifiers("test.Outer.Public", PUBLIC, FINAL, INNER);
            checkModifiers("test.Outer.Protected", PROTECTED, FINAL, INNER);
            checkModifiers("test.Outer.Internal", PUBLIC, FINAL, INNER);
            checkModifiers("test.Outer.Private", PRIVATE, FINAL, INNER);
        }
        public void testModalities() {
            checkModifiers("test.Abstract", PUBLIC, ABSTRACT);
            checkModifiers("test.Open", PUBLIC);
            checkModifiers("test.Final", PUBLIC, FINAL);
        }

        public void testAnnotation() {
            checkModifiers("test.Annotation", PUBLIC, FINAL, ANNOTATION, INTERFACE);
        }

        public void testEnum() {
            checkModifiers("test.Enum", PUBLIC, FINAL, ENUM);
        }
        public void testTrait() {
            checkModifiers("test.Trait", PUBLIC, ABSTRACT, INTERFACE);
        }

        public void testDeprecation() {
            checkModifiers("test.Deprecated", PUBLIC, FINAL, DEPRECATED);
            checkModifiers("test.DeprecatedWithBrackets", PUBLIC, FINAL, DEPRECATED);
        }

        public void testGenericity() {
            checkModifiers("test.Generic1", PUBLIC, FINAL, GENERIC);
            checkModifiers("test.Generic2", PUBLIC, FINAL, GENERIC);
        }
    }

    public static class DeclaredWithGenerics extends KotlinLightClassTest {

        public DeclaredWithGenerics() {
            super(new File("compiler/testData/asJava/lightClasses/DeclaredWithGenerics.kt"));
        }

        public void testGeneric1() throws Exception {
            checkGenericParameter("test.Generic1", 0, "T");
        }

        public void testGeneric1WithBounds() throws Exception {
            checkGenericParameter("test.Generic1WithBounds", 0, "T", "test.Bound1");
        }

        public void testGeneric2() throws Exception {
            checkGenericParameter("test.Generic2", 0, "A");
            checkGenericParameter("test.Generic2", 1, "B");
        }

        public void testGeneric2WithBounds() throws Exception {
            checkGenericParameter("test.Generic2WithBounds", 0, "A", "test.Bound1", "test.Bound2");
            checkGenericParameter("test.Generic2WithBounds", 1, "B", "test.Generic1<A>");
        }
    }

    public static class Package extends KotlinLightClassTest {

        public Package() {
            super(new File("compiler/testData/asJava/lightClasses/Package.kt"));
        }

        public void testPackage() throws Exception {
            checkModifiers("test.namespace", PUBLIC, FINAL);
        }
    }

    private final File path;
    private JavaElementFinder finder;

    protected KotlinLightClassTest(File path) {
        this.path = path;
    }

    @Override
    protected JetCoreEnvironment createEnvironment() {
        CompilerConfiguration configuration = new CompilerConfiguration();

        configuration.add(CommonConfigurationKeys.SOURCE_ROOTS_KEY, path.getPath());

        return new JetCoreEnvironment(getTestRootDisposable(), configuration);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        finder = JavaElementFinder.getInstance(getProject());

        // We need to resolve all the files in order to fill in the trace that sits inside LightClassGenerationSupport
        List<String> paths = getEnvironment().getConfiguration().get(CommonConfigurationKeys.SOURCE_ROOTS_KEY);
        assert paths != null;
        List<JetFile> jetFiles = Lists.newArrayList();
        for (String path : paths) {
            jetFiles.add(JetTestUtils.loadJetFile(getProject(), new File(path)));
        }
        LazyResolveTestUtil.resolveEagerly(jetFiles, getEnvironment());
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

    protected static void checkGenericParameter(PsiClass psiClass, int index, String name, String... bounds) {
        PsiTypeParameter typeParameter = psiClass.getTypeParameters()[index];
        assertEquals(name, typeParameter.getName());
        assertEquals(index, typeParameter.getIndex());
        Set<String> expectedBounds = Sets.newHashSet(bounds);
        Set<String> actualBounds = Sets.newHashSet(Collections2.transform(
                Arrays.asList(typeParameter.getExtendsListTypes()),
                new Function<PsiClassType, String>() {
                    @Override
                    public String apply(PsiClassType input) {
                        return input.getCanonicalText();
                    }
                }
        ));

        assertEquals(expectedBounds, actualBounds);
    }

    protected void checkGenericParameter(String classFqName, int index, String name, String... bounds) {
        checkGenericParameter(findClass(classFqName), index, name, bounds);
    }

    enum ClassProperty {
        PUBLIC(PsiModifier.PUBLIC), 
        PROTECTED(PsiModifier.PROTECTED),
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
                return psiClass.isInterface();
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
        INNER {
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
