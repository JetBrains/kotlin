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

package org.jetbrains.jet.resolve.annotation;


import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetLiteFixture;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.AnonymousFunctionDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class AnnotationDescriptorResolveTest extends JetLiteFixture {
    private static final String PATH = "compiler/testData/resolveAnnotations/testFile.kt";

    private static final FqName NAMESPACE = new FqName("test");

    private BindingContext context;

    @Override
    protected JetCoreEnvironment createEnvironment() {
        return JetTestUtils.createEnvironmentWithMockJdkAndIdeaAnnotations(getTestRootDisposable());
    }

    public void testIntAnnotation() throws IOException {
        String content = getContent("AnnInt(1)");
        String expectedAnnotation = "AnnInt[a = 1.toInt(): jet.Int]";
        doTest(content, expectedAnnotation);
    }

    public void testStringAnnotation() throws IOException {
        String content = getContent("AnnString(\"test\")");
        String expectedAnnotation = "AnnString[a = \"test\": jet.String]";
        doTest(content, expectedAnnotation);
    }

    public void testEnumAnnotation() throws IOException {
        String content = getContent("AnnEnum(MyEnum.A)");
        String expectedAnnotation = "AnnEnum[a = MyEnum.A: test.MyEnum]";
        doTest(content, expectedAnnotation);
    }

    public void testQualifiedEnumAnnotation() throws IOException {
        String content = getContent("AnnEnum(test.MyEnum.A)");
        String expectedAnnotation = "AnnEnum[a = MyEnum.A: test.MyEnum]";
        doTest(content, expectedAnnotation);
    }

    public void testUnqualifiedEnumAnnotation() throws IOException {
        String content = getContent("AnnEnum(A)");
        String expectedAnnotation = "AnnEnum[a = MyEnum.A: test.MyEnum]";
        doTest(content, expectedAnnotation);
    }

    public void testIntArrayAnnotation() throws IOException {
        String content = getContent("AnnIntArray(intArray(1, 2))");
        String expectedAnnotation = "AnnIntArray[a = [1.toInt(), 2.toInt()]: jet.IntArray]";
        doTest(content, expectedAnnotation);
    }

    public void testIntArrayVarargAnnotation() throws IOException {
        String content = getContent("AnnIntVararg(1, 2)");
        String expectedAnnotation = "AnnIntVararg[a = [1.toInt(), 2.toInt()]: jet.IntArray]";
        doTest(content, expectedAnnotation);
    }

    public void testStringArrayVarargAnnotation() throws IOException {
        String content = getContent("AnnStringVararg(\"a\", \"b\")");
        String expectedAnnotation = "AnnStringVararg[a = [\"a\", \"b\"]: jet.Array<jet.String>]";
        doTest(content, expectedAnnotation);
    }

    public void testStringArrayAnnotation() throws IOException {
        String content = getContent("AnnStringArray(array(\"a\"))");
        String expectedAnnotation = "AnnStringArray[a = [\"a\"]: jet.Array<jet.String>]";
        doTest(content, expectedAnnotation);
    }

    public void testEnumArrayAnnotation() throws IOException {
        String content = getContent("AnnArrayOfEnum(array(MyEnum.A))");
        String expectedAnnotation = "AnnArrayOfEnum[a = [MyEnum.A]: jet.Array<test.MyEnum>]";
        doTest(content, expectedAnnotation);
    }

    public void testAnnotationAnnotation() throws Exception {
        String content = getContent("AnnAnn(AnnInt(1))");
        String expectedAnnotation = "AnnAnn[a = AnnInt[a = 1.toInt()]: test.AnnInt]";
        doTest(content, expectedAnnotation);
    }

    public void testJavaClassAnnotation() throws Exception {
        String content = getContent("AnnClass(javaClass<MyClass>())");
        String expectedAnnotation = "AnnClass[a = MyClass.class: java.lang.Class<test.MyClass>]";
        doTest(content, expectedAnnotation);
    }

    private void doTest(@NotNull String content, @NotNull String expectedAnnotation) {
        NamespaceDescriptor test = getNamespaceDescriptor(content);
        ClassDescriptor myClass = getClassDescriptor(test, "MyClass");
        checkDescriptor(expectedAnnotation, myClass);
        checkDescriptor(expectedAnnotation, getClassObjectDescriptor(myClass));
        checkDescriptor(expectedAnnotation, getInnerClassDescriptor(myClass, "InnerClass"));

        FunctionDescriptor foo = getFunctionDescriptor(myClass, "foo");
        checkDescriptor(expectedAnnotation, foo);
        checkDescriptor(expectedAnnotation, getFunctionParameterDescriptor(foo, "param"));

        checkDescriptor(expectedAnnotation, getLocalClassDescriptor("LocalClass"));
        checkDescriptor(expectedAnnotation, getLocalObjectDescriptor("LocalObject"));
        checkDescriptor(expectedAnnotation, getLocalFunDescriptor("localFun"));
        checkDescriptor(expectedAnnotation, getLocalVarDescriptor("localVar"));

        SimpleFunctionDescriptor anonymousFun = getAnonymousFunDescriptor();
        if (anonymousFun instanceof AnonymousFunctionDescriptor) {
            for (ValueParameterDescriptor descriptor : anonymousFun.getValueParameters()) {
                checkDescriptor(expectedAnnotation, descriptor);
            }
        }

        PropertyDescriptor prop = getPropertyDescriptor(myClass, "prop");
        checkDescriptor(expectedAnnotation, prop);
        checkDescriptor(expectedAnnotation, prop.getGetter());
        checkDescriptor(expectedAnnotation, prop.getSetter());

        FunctionDescriptor topFoo = getFunctionDescriptor(test, "topFoo");
        checkDescriptor(expectedAnnotation, topFoo);
        checkDescriptor(expectedAnnotation, getFunctionParameterDescriptor(topFoo, "param"));

        PropertyDescriptor topProp = getPropertyDescriptor(test, "topProp");
        checkDescriptor(expectedAnnotation, topProp);
        checkDescriptor(expectedAnnotation, topProp.getGetter());
        checkDescriptor(expectedAnnotation, topProp.getSetter());

        checkDescriptor(expectedAnnotation, getObjectDescriptor(test, "MyObject"));

        checkDescriptor(expectedAnnotation, getConstructorParameterDescriptor(myClass, "consProp"));
        checkDescriptor(expectedAnnotation, getConstructorParameterDescriptor(myClass, "param"));
    }

    @NotNull
    private static FunctionDescriptor getFunctionDescriptor(@NotNull NamespaceDescriptor namespaceDescriptor, @NotNull String name) {
        Name functionName = Name.identifier(name);
        JetScope memberScope = namespaceDescriptor.getMemberScope();
        Collection<FunctionDescriptor> functions = memberScope.getFunctions(functionName);
        assert functions.size() == 1 : "Failed to find function " + functionName + " in class" + "." + namespaceDescriptor.getName();
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

    @NotNull
    private static PropertyDescriptor getPropertyDescriptor(@NotNull NamespaceDescriptor namespaceDescriptor, @NotNull String name) {
        Name propertyName = Name.identifier(name);
        JetScope memberScope = namespaceDescriptor.getMemberScope();
        Collection<VariableDescriptor> properties = memberScope.getProperties(propertyName);
        assert properties.size() == 1 : "Failed to find property " + propertyName + " in class " + namespaceDescriptor.getName();
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
    private static ClassDescriptor getClassDescriptor(@NotNull NamespaceDescriptor namespaceDescriptor, @NotNull String name) {
        Name className = Name.identifier(name);
        ClassifierDescriptor aClass = namespaceDescriptor.getMemberScope().getClassifier(className);
        assertNotNull("Failed to find class: " + namespaceDescriptor.getName() + "." + className, aClass);
        assert aClass instanceof ClassDescriptor;
        return (ClassDescriptor) aClass;
    }

    @NotNull
    private static ClassDescriptor getObjectDescriptor(@NotNull NamespaceDescriptor namespaceDescriptor, @NotNull String name) {
        Name className = Name.identifier(name);
        ClassDescriptor aClass = namespaceDescriptor.getMemberScope().getObjectDescriptor(className);
        assertNotNull("Failed to find class: " + namespaceDescriptor.getName() + "." + className, aClass);
        return aClass;
    }

    @NotNull
    private static ClassDescriptor getClassObjectDescriptor(@NotNull ClassDescriptor classDescriptor) {
        ClassDescriptor objectDescriptor = classDescriptor.getClassObjectDescriptor();
        assert objectDescriptor != null : "Cannot find class object for class " + classDescriptor.getName();
        return objectDescriptor;
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
        for (ClassDescriptor descriptor : context.getSliceContents(BindingContext.OBJECT_DECLARATION_CLASS).values()) {
            if (descriptor.getName().asString().equals(name)) {
                return descriptor;
            }
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
    private VariableDescriptor getLocalVarDescriptor(@NotNull String name) {
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
    private NamespaceDescriptor getNamespaceDescriptor(@NotNull String content) {
        JetFile ktFile = JetTestUtils.createFile("dummy.kt", content, getProject());
        context = JetTestUtils.analyzeFile(ktFile).getBindingContext();

        NamespaceDescriptor namespaceDescriptor = context.get(BindingContext.FQNAME_TO_NAMESPACE_DESCRIPTOR, NAMESPACE);
        assertNotNull("Failed to find namespace: " + NAMESPACE, namespaceDescriptor);
        return namespaceDescriptor;
    }

    private static String getContent(@NotNull String annotationText) throws IOException {
        File file = new File(PATH);
        String content = JetTestUtils.doLoadFile(file).replaceAll("ANNOTATION", annotationText);
        return content;
    }

    private static void checkDescriptor(String expectedAnnotation, DeclarationDescriptor member) {
        String actual = StringUtil.join(member.getAnnotations(), new Function<AnnotationDescriptor, String>() {
            @Override
            public String fun(AnnotationDescriptor annotationDescriptor) {
                return annotationDescriptor.getType().toString() + DescriptorUtils.getSortedValueArguments(annotationDescriptor, DescriptorRenderer.TEXT);
            }
        }, " ");
        assertEquals("Failed to resolve annotation descriptor for " + member.toString(), expectedAnnotation, actual);
    }
}
