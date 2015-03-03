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

package org.jetbrains.kotlin.resolve.annotation;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.analyzer.AnalysisResult;
import org.jetbrains.kotlin.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.JetAnnotationEntry;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.renderer.DescriptorRenderer;
import org.jetbrains.kotlin.renderer.DescriptorRendererBuilder;
import org.jetbrains.kotlin.renderer.NameShortness;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.scopes.JetScope;
import org.jetbrains.kotlin.test.JetLiteFixture;
import org.jetbrains.kotlin.test.JetTestUtils;
import org.jetbrains.kotlin.types.TypeProjection;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class AbstractAnnotationDescriptorResolveTest extends JetLiteFixture {
    private static final DescriptorRenderer WITH_ANNOTATION_ARGUMENT_TYPES = new DescriptorRendererBuilder()
                                                                                    .setVerbose(true)
                                                                                    .setNameShortness(NameShortness.SHORT)
                                                                                    .build();

    private static final String PATH = "compiler/testData/resolveAnnotations/testFile.kt";

    private static final FqName PACKAGE = new FqName("test");

    protected BindingContext context;

    @Override
    protected JetCoreEnvironment createEnvironment() {
        return JetTestUtils.createEnvironmentWithMockJdkAndIdeaAnnotations(getTestRootDisposable());
    }

    protected void doTest(@NotNull String content, @NotNull String expectedAnnotation) {
        checkAnnotationOnAllExceptLocalDeclarations(content, expectedAnnotation);
        checkAnnotationOnLocalDeclarations(expectedAnnotation);
    }

    protected void checkAnnotationOnAllExceptLocalDeclarations(String content, String expectedAnnotation) {
        JetFile testFile = getFile(content);
        PackageFragmentDescriptor testPackage = getPackage(testFile);

        checkAnnotationsOnFile(expectedAnnotation, testFile);

        ClassDescriptor myClass = getClassDescriptor(testPackage, "MyClass");
        checkDescriptor(expectedAnnotation, myClass);
        ClassDescriptor defaultObjectDescriptor = myClass.getDefaultObjectDescriptor();
        assert defaultObjectDescriptor != null : "Cannot find default object for class " + myClass.getName();
        checkDescriptor(expectedAnnotation, defaultObjectDescriptor);
        checkDescriptor(expectedAnnotation, getInnerClassDescriptor(myClass, "InnerClass"));

        FunctionDescriptor foo = getFunctionDescriptor(myClass, "foo");
        checkAnnotationsOnFunction(expectedAnnotation, foo);

        SimpleFunctionDescriptor anonymousFun = getAnonymousFunDescriptor();
        if (anonymousFun instanceof AnonymousFunctionDescriptor) {
            for (ValueParameterDescriptor descriptor : anonymousFun.getValueParameters()) {
                checkDescriptor(expectedAnnotation, descriptor);
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

    private void checkAnnotationsOnFile(String expectedAnnotation, JetFile file) {
        String actualAnnotation = StringUtil.join(file.getAnnotationEntries(), new Function<JetAnnotationEntry, String>() {
            @Override
            public String fun(JetAnnotationEntry annotationEntry) {
                AnnotationDescriptor annotationDescriptor = context.get(BindingContext.ANNOTATION, annotationEntry);
                assertNotNull(annotationDescriptor);
                return WITH_ANNOTATION_ARGUMENT_TYPES.renderAnnotation(annotationDescriptor);
            }
        }, " ");

        assertEquals(expectedAnnotation, actualAnnotation);
    }

    private void checkAnnotationOnLocalDeclarations(String expectedAnnotation) {
        checkDescriptor(expectedAnnotation, getLocalClassDescriptor("LocalClass"));
        checkDescriptor(expectedAnnotation, getLocalObjectDescriptor("LocalObject"));
        checkDescriptor(expectedAnnotation, getLocalFunDescriptor("localFun"));
        checkDescriptor(expectedAnnotation, getLocalVarDescriptor(context, "localVar"));
    }

    private static void checkAnnotationsOnProperty(String expectedAnnotation, PropertyDescriptor prop) {
        checkDescriptor(expectedAnnotation, prop);
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
        JetScope memberScope = packageView.getMemberScope();
        Collection<FunctionDescriptor> functions = memberScope.getFunctions(functionName);
        assert functions.size() == 1 : "Failed to find function " + functionName + " in class" + "." + packageView.getName();
        return functions.iterator().next();
    }

    @NotNull
    private static FunctionDescriptor getFunctionDescriptor(@NotNull ClassDescriptor classDescriptor, @NotNull String name) {
        Name functionName = Name.identifier(name);
        JetScope memberScope = classDescriptor.getMemberScope(Collections.<TypeProjection>emptyList());
        Collection<FunctionDescriptor> functions = memberScope.getFunctions(functionName);
        assert functions.size() == 1 : "Failed to find function " + functionName + " in class" + "." + classDescriptor.getName();
        return functions.iterator().next();
    }

    @Nullable
    protected static PropertyDescriptor getPropertyDescriptor(@NotNull PackageFragmentDescriptor packageView, @NotNull String name, boolean failOnMissing) {
        Name propertyName = Name.identifier(name);
        JetScope memberScope = packageView.getMemberScope();
        Collection<VariableDescriptor> properties = memberScope.getProperties(propertyName);
        if (properties.isEmpty()) {
            for (DeclarationDescriptor descriptor : memberScope.getAllDescriptors()) {
                if (descriptor instanceof ClassDescriptor) {
                    Collection<VariableDescriptor> classProperties =
                            ((ClassDescriptor) descriptor).getMemberScope(Collections.<TypeProjection>emptyList())
                                    .getProperties(propertyName);
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
        return (PropertyDescriptor) properties.iterator().next();
    }

    @NotNull
    private static PropertyDescriptor getPropertyDescriptor(@NotNull ClassDescriptor classDescriptor, @NotNull String name) {
        Name propertyName = Name.identifier(name);
        JetScope memberScope = classDescriptor.getMemberScope(Collections.<TypeProjection>emptyList());
        Collection<VariableDescriptor> properties = memberScope.getProperties(propertyName);
        assert properties.size() == 1 : "Failed to find property " + propertyName + " in class " + classDescriptor.getName();
        return (PropertyDescriptor) properties.iterator().next();
    }

    @NotNull
    protected static ClassDescriptor getClassDescriptor(@NotNull PackageFragmentDescriptor packageView, @NotNull String name) {
        Name className = Name.identifier(name);
        ClassifierDescriptor aClass = packageView.getMemberScope().getClassifier(className);
        assertNotNull("Failed to find class: " + packageView.getName() + "." + className, aClass);
        assert aClass instanceof ClassDescriptor : "Not a class: " + aClass;
        return (ClassDescriptor) aClass;
    }

    @NotNull
    private static ClassDescriptor getInnerClassDescriptor(@NotNull ClassDescriptor classDescriptor, @NotNull String name) {
        Name propertyName = Name.identifier(name);
        JetScope memberScope = classDescriptor.getMemberScope(Collections.<TypeProjection>emptyList());
        ClassifierDescriptor innerClass = memberScope.getClassifier(propertyName);
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
        if (localClassDescriptor.getKind() == ClassKind.OBJECT) {
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
        Collection<ConstructorDescriptor> constructors = classDescriptor.getConstructors();
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
    protected JetFile getFile(@NotNull String content) {
        JetFile ktFile = JetTestUtils.createFile("dummy.kt", content, getProject());
        AnalysisResult analysisResult = analyzeFile(ktFile);
        context = analysisResult.getBindingContext();

        return ktFile;
    }

    @NotNull
    protected PackageFragmentDescriptor getPackage(@NotNull JetFile ktFile) {
        PackageFragmentDescriptor packageFragment = context.get(BindingContext.FILE_TO_PACKAGE_FRAGMENT, ktFile);
        assertNotNull("Failed to find package: " + PACKAGE, packageFragment);
        return packageFragment;
    }

    @NotNull
    protected PackageFragmentDescriptor getPackage(@NotNull String content) {
        return getPackage(getFile(content));
    }

    @NotNull
    protected AnalysisResult analyzeFile(@NotNull JetFile ktFile) {
        return JetTestUtils.analyzeFile(ktFile);
    }

    protected static String getContent(@NotNull String annotationText) throws IOException {
        File file = new File(PATH);
        return JetTestUtils.doLoadFile(file).replaceAll("ANNOTATION", annotationText);
    }

    private static String renderAnnotations(Annotations annotations) {
        return StringUtil.join(annotations, new Function<AnnotationDescriptor, String>() {
            @Override
            public String fun(AnnotationDescriptor annotationDescriptor) {
                return WITH_ANNOTATION_ARGUMENT_TYPES.renderAnnotation(annotationDescriptor);
            }
        }, " ");
    }

    protected static void checkDescriptor(String expectedAnnotation, DeclarationDescriptor member) {
        String actual = getAnnotations(member);
        assertEquals("Failed to resolve annotation descriptor for " + member.toString(), expectedAnnotation, actual);
    }

    @NotNull
    protected static String getAnnotations(DeclarationDescriptor member) {
        return renderAnnotations(member.getAnnotations());
    }

    @Override
    public void tearDown() throws Exception {
        context = null;
        super.tearDown();
    }
}
