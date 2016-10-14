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

package org.jetbrains.kotlin.resolve.annotation;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Function;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.analyzer.AnalysisResult;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget;
import org.jetbrains.kotlin.descriptors.annotations.AnnotationWithTarget;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor;
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl;
import org.jetbrains.kotlin.incremental.components.NoLookupLocation;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.KtAnnotationEntry;
import org.jetbrains.kotlin.psi.KtAnnotationUseSiteTarget;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.renderer.ClassifierNamePolicy;
import org.jetbrains.kotlin.renderer.DescriptorRenderer;
import org.jetbrains.kotlin.renderer.DescriptorRendererModifier;
import org.jetbrains.kotlin.renderer.DescriptorRendererOptions;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.scopes.MemberScope;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.KotlinTestWithEnvironment;
import org.jetbrains.kotlin.types.TypeProjection;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.resolve.DescriptorUtils.isNonCompanionObject;

public abstract class AbstractAnnotationDescriptorResolveTest extends KotlinTestWithEnvironment {
    private static final DescriptorRenderer WITH_ANNOTATION_ARGUMENT_TYPES = DescriptorRenderer.Companion.withOptions(
            new Function1<DescriptorRendererOptions, Unit>() {
                @Override
                public Unit invoke(DescriptorRendererOptions options) {
                    options.setVerbose(true);
                    options.setIncludeAnnotationArguments(true);
                    options.setClassifierNamePolicy(ClassifierNamePolicy.SHORT.INSTANCE);
                    options.setModifiers(DescriptorRendererModifier.ALL);
                    return Unit.INSTANCE;
                }
            }
    );

    private static final String PATH = "compiler/testData/resolveAnnotations/testFile.kt";

    private static final FqName PACKAGE = new FqName("test");

    protected BindingContext context;

    @Override
    protected KotlinCoreEnvironment createEnvironment() {
        return KotlinTestUtils.createEnvironmentWithMockJdkAndIdeaAnnotations(getTestRootDisposable());
    }

    protected void doTest(@NotNull String content, @NotNull String expectedAnnotation) {
        checkAnnotationOnAllExceptLocalDeclarations(content, expectedAnnotation);
        checkAnnotationOnLocalDeclarations(expectedAnnotation);
    }

    protected void checkAnnotationOnAllExceptLocalDeclarations(String content, String expectedAnnotation) {
        KtFile testFile = getFile(content);
        PackageFragmentDescriptor testPackage = getPackage(testFile);

        checkAnnotationsOnFile(expectedAnnotation, testFile);

        ClassDescriptor myClass = getClassDescriptor(testPackage, "MyClass");
        checkDescriptor(expectedAnnotation, myClass);
        ClassDescriptor companionObjectDescriptor = myClass.getCompanionObjectDescriptor();
        assert companionObjectDescriptor != null : "Cannot find companion object for class " + myClass.getName();
        checkDescriptor(expectedAnnotation, companionObjectDescriptor);
        checkDescriptor(expectedAnnotation, getInnerClassDescriptor(myClass, "InnerClass"));

        FunctionDescriptor foo = getFunctionDescriptor(myClass, "foo");
        checkAnnotationsOnFunction(expectedAnnotation, foo);

        SimpleFunctionDescriptor anonymousFun = getAnonymousFunDescriptor();
        if (anonymousFun instanceof AnonymousFunctionDescriptor) {
            for (ValueParameterDescriptor descriptor : anonymousFun.getValueParameters()) {
                List<VariableDescriptor> destructuringVariables = ValueParameterDescriptorImpl.getDestructuringVariablesOrNull(descriptor);
                if (destructuringVariables == null) continue;
                for (VariableDescriptor entry : destructuringVariables) {
                    checkDescriptor(expectedAnnotation, entry);
                }
            }
        }

        PropertyDescriptor prop = getPropertyDescriptor(myClass, "prop");
        checkAnnotationsOnProperty(expectedAnnotation, prop);

        FunctionDescriptor topFoo = getFunctionDescriptor(testPackage, "topFoo");
        checkAnnotationsOnFunction(expectedAnnotation, topFoo);

        PropertyDescriptor topProp = getPropertyDescriptor(testPackage, "topProp", true);
        checkAnnotationsOnProperty(expectedAnnotation, topProp);

        checkDescriptor(expectedAnnotation, getClassDescriptor(testPackage, "MyObject"));

        checkDescriptor(expectedAnnotation, getConstructorParameterDescriptor(myClass, "consProp"));
        checkDescriptor(expectedAnnotation, getConstructorParameterDescriptor(myClass, "param"));
    }

    private void checkAnnotationsOnFile(String expectedAnnotation, KtFile file) {
        String actualAnnotation = StringUtil.join(file.getAnnotationEntries(), new Function<KtAnnotationEntry, String>() {
            @Override
            public String fun(KtAnnotationEntry annotationEntry) {
                AnnotationDescriptor annotationDescriptor = context.get(BindingContext.ANNOTATION, annotationEntry);
                assertNotNull(annotationDescriptor);

                KtAnnotationUseSiteTarget target = annotationEntry.getUseSiteTarget();

                if (target != null) {
                    return WITH_ANNOTATION_ARGUMENT_TYPES.renderAnnotation(
                            annotationDescriptor, target.getAnnotationUseSiteTarget());
                }

                return WITH_ANNOTATION_ARGUMENT_TYPES.renderAnnotation(annotationDescriptor, null);
            }
        }, " ");

        String expectedAnnotationWithTarget = "@" + AnnotationUseSiteTarget.FILE.getRenderName() + ":" + expectedAnnotation.substring(1);

        assertEquals(expectedAnnotationWithTarget, actualAnnotation);
    }

    private void checkAnnotationOnLocalDeclarations(String expectedAnnotation) {
        checkDescriptor(expectedAnnotation, getLocalClassDescriptor("LocalClass"));
        checkDescriptor(expectedAnnotation, getLocalObjectDescriptor("LocalObject"));
        checkDescriptor(expectedAnnotation, getLocalFunDescriptor("localFun"));
        checkDescriptor(expectedAnnotation, getLocalVarDescriptor(context, "localVar"));
    }

    private static void checkAnnotationsOnProperty(String expectedAnnotation, PropertyDescriptor prop) {
        checkDescriptorWithTarget(expectedAnnotation, prop, AnnotationUseSiteTarget.FIELD);
        checkDescriptor(expectedAnnotation, prop.getGetter());
        PropertySetterDescriptor propSetter = prop.getSetter();
        assertNotNull(propSetter);
        checkAnnotationsOnFunction(expectedAnnotation, propSetter);
    }

    private static void checkAnnotationsOnFunction(String expectedAnnotation, FunctionDescriptor foo) {
        checkDescriptor(expectedAnnotation, foo);
        checkDescriptor(expectedAnnotation, getFunctionParameterDescriptor(foo, "param"));
    }

    @NotNull
    protected static FunctionDescriptor getFunctionDescriptor(@NotNull PackageFragmentDescriptor packageView, @NotNull String name) {
        Name functionName = Name.identifier(name);
        MemberScope memberScope = packageView.getMemberScope();
        Collection<SimpleFunctionDescriptor> functions = memberScope.getContributedFunctions(functionName, NoLookupLocation.FROM_TEST);
        assert functions.size() == 1 : "Failed to find function " + functionName + " in class" + "." + packageView.getName();
        return functions.iterator().next();
    }

    @NotNull
    private static FunctionDescriptor getFunctionDescriptor(@NotNull ClassDescriptor classDescriptor, @NotNull String name) {
        Name functionName = Name.identifier(name);
        MemberScope memberScope = classDescriptor.getMemberScope(Collections.<TypeProjection>emptyList());
        Collection<SimpleFunctionDescriptor> functions = memberScope.getContributedFunctions(functionName, NoLookupLocation.FROM_TEST);
        assert functions.size() == 1 : "Failed to find function " + functionName + " in class" + "." + classDescriptor.getName();
        return functions.iterator().next();
    }

    @Nullable
    protected static PropertyDescriptor getPropertyDescriptor(@NotNull PackageFragmentDescriptor packageView, @NotNull String name, boolean failOnMissing) {
        Name propertyName = Name.identifier(name);
        MemberScope memberScope = packageView.getMemberScope();
        Collection<PropertyDescriptor> properties = memberScope.getContributedVariables(propertyName, NoLookupLocation.FROM_TEST);
        if (properties.isEmpty()) {
            for (DeclarationDescriptor descriptor : DescriptorUtils.getAllDescriptors(memberScope)) {
                if (descriptor instanceof ClassDescriptor) {
                    Collection<PropertyDescriptor> classProperties =
                            ((ClassDescriptor) descriptor).getMemberScope(Collections.<TypeProjection>emptyList())
                                    .getContributedVariables(propertyName, NoLookupLocation.FROM_TEST);
                    if (!classProperties.isEmpty()) {
                        properties = classProperties;
                        break;
                    }
                }
            }
        }
        if (failOnMissing) {
            assert properties.size() == 1 : "Failed to find property " + propertyName + " in class " + packageView.getName();
        }
        else if (properties.size() != 1) {
            return null;
        }
        return properties.iterator().next();
    }

    @NotNull
    private static PropertyDescriptor getPropertyDescriptor(@NotNull ClassDescriptor classDescriptor, @NotNull String name) {
        Name propertyName = Name.identifier(name);
        MemberScope memberScope = classDescriptor.getMemberScope(Collections.<TypeProjection>emptyList());
        Collection<PropertyDescriptor> properties = memberScope.getContributedVariables(propertyName, NoLookupLocation.FROM_TEST);
        assert properties.size() == 1 : "Failed to find property " + propertyName + " in class " + classDescriptor.getName();
        return properties.iterator().next();
    }

    @NotNull
    protected static ClassDescriptor getClassDescriptor(@NotNull PackageFragmentDescriptor packageView, @NotNull String name) {
        Name className = Name.identifier(name);
        ClassifierDescriptor aClass = packageView.getMemberScope().getContributedClassifier(className, NoLookupLocation.FROM_TEST);
        assertNotNull("Failed to find class: " + packageView.getName() + "." + className, aClass);
        assert aClass instanceof ClassDescriptor : "Not a class: " + aClass;
        return (ClassDescriptor) aClass;
    }

    @NotNull
    private static ClassDescriptor getInnerClassDescriptor(@NotNull ClassDescriptor classDescriptor, @NotNull String name) {
        Name propertyName = Name.identifier(name);
        MemberScope memberScope = classDescriptor.getMemberScope(Collections.<TypeProjection>emptyList());
        ClassifierDescriptor innerClass = memberScope.getContributedClassifier(propertyName, NoLookupLocation.FROM_TEST);
        assert innerClass instanceof ClassDescriptor : "Failed to find inner class " +
                                                       propertyName +
                                                       " in class " +
                                                       classDescriptor.getName();
        return (ClassDescriptor) innerClass;
    }

    @NotNull
    private ClassDescriptor getLocalClassDescriptor(@NotNull String name) {
        for (ClassDescriptor descriptor : context.getSliceContents(BindingContext.CLASS).values()) {
            if (descriptor.getName().asString().equals(name)) {
                return descriptor;
            }
        }

        fail("Failed to find local class " + name);
        return null;
    }

    @NotNull
    private ClassDescriptor getLocalObjectDescriptor(@NotNull String name) {
        ClassDescriptor localClassDescriptor = getLocalClassDescriptor(name);
        if (isNonCompanionObject(localClassDescriptor)) {
            return localClassDescriptor;
        }

        fail("Failed to find local object " + name);
        return null;
    }

    @NotNull
    private SimpleFunctionDescriptor getLocalFunDescriptor(@NotNull String name) {
        for (SimpleFunctionDescriptor descriptor : context.getSliceContents(BindingContext.FUNCTION).values()) {
            if (descriptor.getName().asString().equals(name)) {
                return descriptor;
            }
        }

        fail("Failed to find local fun " + name);
        return null;
    }

    @NotNull
    protected static VariableDescriptor getLocalVarDescriptor(@NotNull BindingContext context, @NotNull String name) {
        for (VariableDescriptor descriptor : context.getSliceContents(BindingContext.VARIABLE).values()) {
            if (descriptor.getName().asString().equals(name)) {
                return descriptor;
            }
        }

        fail("Failed to find local variable " + name);
        return null;
    }

    @NotNull
    private SimpleFunctionDescriptor getAnonymousFunDescriptor() {
        for (SimpleFunctionDescriptor descriptor : context.getSliceContents(BindingContext.FUNCTION).values()) {
            if (descriptor instanceof AnonymousFunctionDescriptor) {
                return descriptor;
            }
        }

        fail("Failed to find anonymous fun");
        return null;
    }

    @NotNull
    private static ValueParameterDescriptor getConstructorParameterDescriptor(
            @NotNull ClassDescriptor classDescriptor,
            @NotNull String name
    ) {
        ConstructorDescriptor constructorDescriptor = getConstructorDescriptor(classDescriptor);
        ValueParameterDescriptor parameter = findValueParameter(constructorDescriptor.getValueParameters(), name);
        assertNotNull("Cannot find constructor parameter with name " + name, parameter);
        return parameter;
    }

    @NotNull
    private static ConstructorDescriptor getConstructorDescriptor(@NotNull ClassDescriptor classDescriptor) {
        Collection<ClassConstructorDescriptor> constructors = classDescriptor.getConstructors();
        assert constructors.size() == 1;
        return constructors.iterator().next();
    }

    private static ValueParameterDescriptor findValueParameter(List<ValueParameterDescriptor> parameters, String name) {
        for (ValueParameterDescriptor parameter : parameters) {
            if (parameter.getName().asString().equals(name)) {
                return parameter;
            }
        }
        return null;
    }

    @NotNull
    private static ValueParameterDescriptor getFunctionParameterDescriptor(
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull String name
    ) {
        ValueParameterDescriptor parameter = findValueParameter(functionDescriptor.getValueParameters(), name);
        assertNotNull("Cannot find function parameter with name " + name, parameter);
        return parameter;
    }

    @NotNull
    protected KtFile getFile(@NotNull String content) {
        KtFile ktFile = KotlinTestUtils.createFile("dummy.kt", content, getProject());
        AnalysisResult analysisResult = KotlinTestUtils.analyzeFile(ktFile, getEnvironment());
        context = analysisResult.getBindingContext();

        return ktFile;
    }

    @NotNull
    protected PackageFragmentDescriptor getPackage(@NotNull KtFile ktFile) {
        PackageFragmentDescriptor packageFragment = context.get(BindingContext.FILE_TO_PACKAGE_FRAGMENT, ktFile);
        assertNotNull("Failed to find package: " + PACKAGE, packageFragment);
        return packageFragment;
    }

    @NotNull
    protected PackageFragmentDescriptor getPackage(@NotNull String content) {
        return getPackage(getFile(content));
    }

    protected static String getContent(@NotNull String annotationText) throws IOException {
        File file = new File(PATH);
        return KotlinTestUtils.doLoadFile(file).replaceAll("ANNOTATION", annotationText);
    }

    private static String renderAnnotations(Annotations annotations, @Nullable final AnnotationUseSiteTarget defaultTarget) {
        return StringUtil.join(annotations.getAllAnnotations(), new Function<AnnotationWithTarget, String>() {
            @Override
            public String fun(AnnotationWithTarget annotationWithTarget) {
                AnnotationUseSiteTarget targetToRender = annotationWithTarget.getTarget();
                if (targetToRender == defaultTarget) {
                    targetToRender = null;
                }

                return WITH_ANNOTATION_ARGUMENT_TYPES.renderAnnotation(annotationWithTarget.getAnnotation(), targetToRender);
            }
        }, " ");
    }

    protected static void checkDescriptor(String expectedAnnotation, DeclarationDescriptor member) {
        String actual = getAnnotations(member);
        assertEquals("Failed to resolve annotation descriptor for " + member.toString(), expectedAnnotation, actual);
    }

    private static void checkDescriptorWithTarget(String expectedAnnotation, DeclarationDescriptor member, AnnotationUseSiteTarget target) {
        String actual = renderAnnotations(member.getAnnotations(), target);
        assertEquals("Failed to resolve annotation descriptor for " + member.toString(), expectedAnnotation, actual);
    }

    @NotNull
    protected static String getAnnotations(DeclarationDescriptor member) {
        return renderAnnotations(member.getAnnotations(), null);
    }

    @Override
    public void tearDown() throws Exception {
        context = null;
        super.tearDown();
    }
}
