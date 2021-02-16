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

import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.analyzer.AnalysisResult;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor;
import org.jetbrains.kotlin.descriptors.ModuleDescriptor;
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor;
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages;
import org.jetbrains.kotlin.incremental.components.NoLookupLocation;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtPsiFactoryKt;
import org.jetbrains.kotlin.psi.KtTypeReference;
import org.jetbrains.kotlin.renderer.DescriptorRenderer;
import org.jetbrains.kotlin.resolve.*;
import org.jetbrains.kotlin.resolve.calls.results.TypeSpecificityComparator;
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil;
import org.jetbrains.kotlin.resolve.scopes.*;
import org.jetbrains.kotlin.resolve.scopes.utils.ScopeUtilsKt;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.KotlinTestWithEnvironment;
import org.jetbrains.kotlin.tests.di.ContainerForTests;
import org.jetbrains.kotlin.tests.di.InjectionKt;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
public class TypeSubstitutorTest extends KotlinTestWithEnvironment {
    private LexicalScope scope;
    private ContainerForTests container;

    @Override
    protected KotlinCoreEnvironment createEnvironment() {
        return createEnvironmentWithMockJdk(ConfigurationKind.JDK_ONLY);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        container = InjectionKt.createContainerForTests(getProject(), KotlinTestUtils.createEmptyModule());
        scope = getContextScope();
    }

    @Override
    protected void tearDown() throws Exception {
        container = null;
        scope = null;
        super.tearDown();
    }

    private LexicalScope getContextScope() throws IOException {
        // todo comments
        String text = FileUtil.loadFile(new File("compiler/testData/type-substitutor.kt"), true);
        KtFile ktFile = KtPsiFactoryKt.KtPsiFactory(getProject()).createFile(text);
        AnalysisResult analysisResult = JvmResolveUtil.analyze(ktFile, getEnvironment());
        ModuleDescriptor module = analysisResult.getModuleDescriptor();

        LexicalScope topLevelScope = analysisResult.getBindingContext().get(BindingContext.LEXICAL_SCOPE, ktFile);
        ClassifierDescriptor contextClass =
                ScopeUtilsKt.findClassifier(topLevelScope, Name.identifier("___Context"), NoLookupLocation.FROM_TEST);
        assert contextClass instanceof ClassDescriptor;
        LocalRedeclarationChecker redeclarationChecker =
                new ThrowingLocalRedeclarationChecker(new OverloadChecker(TypeSpecificityComparator.NONE.INSTANCE));
        LexicalScope typeParameters = new LexicalScopeImpl(
                topLevelScope, module, false, Collections.emptyList(), LexicalScopeKind.SYNTHETIC,
                redeclarationChecker,
                handler -> {
                    for (TypeParameterDescriptor parameterDescriptor : contextClass.getTypeConstructor().getParameters()) {
                        handler.addClassifierDescriptor(parameterDescriptor);
                    }
                    return Unit.INSTANCE;
                }
        );
        return LexicalChainedScope.Companion.create(
                typeParameters, module, false, Collections.emptyList(), LexicalScopeKind.SYNTHETIC,
                contextClass.getDefaultType().getMemberScope(),
                module.getBuiltIns().getBuiltInsPackageScope()
        );
    }

    private void doTest(@Nullable String expectedTypeStr, String initialTypeStr, Pair<String, String>... substitutionStrs) {
        KotlinType initialType = resolveType(initialTypeStr);

        Map<TypeConstructor, TypeProjection> map = stringsToSubstitutionMap(substitutionStrs);
        TypeSubstitutor substitutor = TypeSubstitutor.create(map);

        KotlinType result = substitutor.substitute(initialType, Variance.INVARIANT);

        if (expectedTypeStr == null) {
            assertNull(result);
        }
        else {
            assertNotNull(result);
            assertEquals(expectedTypeStr, DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(result));
        }
    }

    private Map<TypeConstructor, TypeProjection> stringsToSubstitutionMap(Pair<String, String>[] substitutionStrs) {
        Map<TypeConstructor, TypeProjection> map = new HashMap();
        for (Pair<String, String> pair : substitutionStrs) {
            String typeParameterName = pair.first;
            String replacementProjectionString = pair.second;

            ClassifierDescriptor classifier = ScopeUtilsKt.findClassifier(scope, Name.identifier(typeParameterName), NoLookupLocation.FROM_TEST);
            assertNotNull("No type parameter named " + typeParameterName, classifier);
            assertTrue(typeParameterName + " is not a type parameter: " + classifier, classifier instanceof TypeParameterDescriptor);

            String typeStr = "C<" + replacementProjectionString + ">";
            KotlinType typeWithArgument = resolveType(typeStr);
            assert !typeWithArgument.getArguments().isEmpty() : "No arguments: " + typeWithArgument + " from " + typeStr;

            map.put(classifier.getTypeConstructor(), typeWithArgument.getArguments().get(0));
        }
        return map;
    }

    private KotlinType resolveType(String typeStr) {
        KtTypeReference jetTypeReference = KtPsiFactoryKt.KtPsiFactory(getProject()).createType(typeStr);
        AnalyzingUtils.checkForSyntacticErrors(jetTypeReference);
        BindingTrace trace = new BindingTraceContext();
        KotlinType type = container.getTypeResolver().resolveType(scope, jetTypeReference, trace, true);
        if (!trace.getBindingContext().getDiagnostics().isEmpty()) {
            fail("Errors:\n" + StringUtil.join(trace.getBindingContext().getDiagnostics(), DefaultErrorMessages::render, "\n"));
        }
        return type;
    }

    @NotNull
    private static Pair<String, String> map(String typeParameterName, String replacementProjectionString) {
        return Pair.create(typeParameterName, replacementProjectionString);
    }

    public void testNoOccurrence() throws Exception {
        doTest(
                "C<Int>",
                "C<Int>",
                map("T", "String")
        );
    }

    public void testSimpleOccurrence() throws Exception {
        doTest(
                "C<String>",
                "C<T>",
                map("T", "String")
        );
    }

    public void testSimpleOutProjectionInReplacement() throws Exception {
        doTest(
                "C<out String>",
                "C<T>",
                map("T", "out String")
        );
    }

    public void testSimpleInProjectionInReplacement() throws Exception {
        doTest(
                "C<in String>",
                "C<T>",
                map("T", "in String")
        );
    }

    public void testSimpleOutProjectionInSubject() throws Exception {
        doTest(
                "C<out String>",
                "C<out T>",
                map("T", "String")
        );
    }

    public void testSimpleInProjectionInSubject() throws Exception {
        doTest(
                "C<in String>",
                "C<in T>",
                map("T", "String")
        );
    }

    public void testOutOutProjection() throws Exception {
        doTest(
                "C<out String>",
                "C<out T>",
                map("T", "out String")
        );
    }

    public void testInInProjection() throws Exception {
        doTest(
                "C<in String>",
                "C<in T>",
                map("T", "in String")
        );
    }

    public void testInOutProjection() throws Exception {
        doTest(
                null,
                "C<in T>",
                map("T", "out String")
        );
    }

    public void testOutInProjection() throws Exception {
        doTest(
                "C<out Any?>",
                "C<out T>",
                map("T", "in String")
        );
    }

    public void testOutOutProjectionDeclarationSite() throws Exception {
        doTest(
                "Out<String>",
                "Out<T>",
                map("T", "out String")
        );
    }

    public void testInInProjectionDeclarationSite() throws Exception {
        doTest(
                "In<String>",
                "In<T>",
                map("T", "in String")
        );
    }

    public void testInOutProjectionDeclarationSite() throws Exception {
        doTest(
                "In<*>",
                "In<T>",
                map("T", "out String")
        );
    }

    public void testOutInProjectionDeclarationSite() throws Exception {
        doTest(
                "Out<*>",
                "Out<T>",
                map("T", "in String")
        );
    }

    public void testTwoParameters() throws Exception {
        doTest(
                "P<Int, String>",
                "P<T, R>",
                map("T", "Int"),
                map("R", "String")
        );
    }

    public void testDeepType() throws Exception {
        doTest(
                "C<P<Int, P<Int, String>>>",
                "C<P<T,   P<T,   R>>>",
                map("T", "Int"),
                map("R", "String")
        );
    }

    public void testShallowType() throws Exception {
        doTest(
                "String",
                "T",
                map("T", "String")
        );
    }

    public void testShallowTypeNullable() throws Exception {
        doTest(
                "String?",
                "T?",
                map("T", "String")
        );
    }

    public void testShallowTypeNullableReplacement() throws Exception {
        doTest(
                "String?",
                "T",
                map("T", "String?")
        );
    }

    public void testShallowTypeNullableOnBothEnds() throws Exception {
        doTest(
                "String?",
                "T?",
                map("T", "String?")
        );
    }

    public void testNothingType() throws Exception {
        doTest(
                "Nothing",
                "Nothing",
                map("T", "String")
        );
    }

    public void testCallSiteNullable() throws Exception {
        doTest(
                "C<String?>",
                "C<T?>",
                map("T", "String")
        );
    }

    public void testReplacementNullable() throws Exception {
        doTest(
                "C<String?>",
                "C<T>",
                map("T", "String?")
        );
    }

    public void testCallSiteNullableWithProjection() throws Exception {
        doTest(
                "C<out String?>",
                "C<T?>",
                map("T", "out String")
        );
    }

    public void testReplacementNullableWithProjection() throws Exception {
        doTest(
                "C<out String?>",
                "C<out T>",
                map("T", "String?")
        );
    }

    public void testCallSiteNullableWithConsumedProjection() throws Exception {
        doTest(
                "Out<String?>",
                "Out<T?>",
                map("T", "out String")
        );
    }

    public void testReplacementNullableConsumedProjection() throws Exception {
        doTest(
                "In<String?>",
                "In<T>",
                map("T", "in String?")
        );
    }

    //public void testTwoParametersInChain() throws Exception {
    //    doTest(
    //            "P<Int, Int>",
    //            "P<T, R>",
    //            map("T", "Int"),
    //            map("R", "T")
    //    );
    //}

    public void testStarProjection() throws Exception {
        doTest(
                "Rec<*>",
                "Rec<*>",
                map("T", "String")
        );
    }

    public void testStarProjectionOut() throws Exception {
        doTest(
                "Out<*>",
                "Out<*>",
                map("T", "String")
        );
    }

}
