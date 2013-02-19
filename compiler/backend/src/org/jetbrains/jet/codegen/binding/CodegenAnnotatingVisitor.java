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

package org.jetbrains.jet.codegen.binding;

import com.intellij.util.containers.Stack;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.ClassDescriptorImpl;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.jetbrains.jet.codegen.CodegenUtil.peekFromStack;
import static org.jetbrains.jet.codegen.binding.CodegenBinding.*;
import static org.jetbrains.jet.lang.resolve.BindingContext.*;

class CodegenAnnotatingVisitor extends JetVisitorVoid {
    private final Map<String, Integer> anonymousSubclassesCount = new HashMap<String, Integer>();

    private final Stack<ClassDescriptor> classStack = new Stack<ClassDescriptor>();
    private final Stack<String> nameStack = new Stack<String>();
    private final BindingTrace bindingTrace;
    private final BindingContext bindingContext;

    public CodegenAnnotatingVisitor(BindingTrace bindingTrace) {
        this.bindingTrace = bindingTrace;
        this.bindingContext = bindingTrace.getBindingContext();
    }

    private ClassDescriptor recordClassForFunction(FunctionDescriptor funDescriptor) {
        ClassDescriptor classDescriptor;
        int arity = funDescriptor.getValueParameters().size();

        classDescriptor = new ClassDescriptorImpl(
                funDescriptor.getContainingDeclaration(),
                Collections.<AnnotationDescriptor>emptyList(),
                Modality.FINAL,
                Name.special("<closure>"));
        ((ClassDescriptorImpl)classDescriptor).initialize(
                false,
                Collections.<TypeParameterDescriptor>emptyList(),
                Collections.singleton((funDescriptor.getReceiverParameter() != null
                                       ? KotlinBuiltIns.getInstance().getExtensionFunction(arity)
                                       : KotlinBuiltIns.getInstance().getFunction(arity)).getDefaultType()), JetScope.EMPTY,
                Collections.<ConstructorDescriptor>emptySet(),
                null,
                false);

        assert PsiCodegenPredictor.checkPredictedClassNameForFun(bindingContext, funDescriptor, classDescriptor);
        bindingTrace.record(CLASS_FOR_FUNCTION, funDescriptor, classDescriptor);
        return classDescriptor;
    }

    private String inventAnonymousClassName(JetElement declaration) {
        String top = peekFromStack(nameStack);
        Integer cnt = anonymousSubclassesCount.get(top);
        if (cnt == null) {
            cnt = 0;
        }
        String name = top + "$" + (cnt + 1);
        ClassDescriptor descriptor = bindingContext.get(CLASS, declaration);
        if (descriptor == null) {
            if (declaration instanceof JetFunctionLiteralExpression ||
                declaration instanceof JetNamedFunction ||
                declaration instanceof JetObjectLiteralExpression) {
            }
            else {
                throw new IllegalStateException(
                        "Class-less declaration which is not JetFunctionLiteralExpression|JetNamedFunction|JetObjectLiteralExpression : " +
                        declaration.getClass().getName());
            }
        }
        anonymousSubclassesCount.put(top, cnt + 1);

        return name;
    }

    @Override
    public void visitJetElement(JetElement element) {
        super.visitJetElement(element);
        element.acceptChildren(this);
    }

    @Override
    public void visitJetFile(JetFile file) {
        if (file.isScript()) {
            //noinspection ConstantConditions
            final ClassDescriptor classDescriptor =
                    bindingContext.get(CLASS_FOR_FUNCTION, bindingContext.get(SCRIPT, file.getScript()));
            classStack.push(classDescriptor);
            //noinspection ConstantConditions
            nameStack.push(classNameForScriptPsi(bindingContext, file.getScript()).getInternalName());
        }
        else {
            nameStack.push(JvmClassName.byFqNameWithoutInnerClasses(JetPsiUtil.getFQName(file)).getInternalName());
        }
        file.acceptChildren(this);
        nameStack.pop();
        if (file.isScript()) {
            classStack.pop();
        }
    }

    @Override
    public void visitEnumEntry(JetEnumEntry enumEntry) {
        ClassDescriptor descriptor = bindingContext.get(CLASS, enumEntry);
        assert descriptor != null;

        final boolean trivial = enumEntry.getDeclarations().isEmpty();
        if (!trivial) {
            bindingTrace.record(ENUM_ENTRY_CLASS_NEED_SUBCLASS, descriptor);
            super.visitEnumEntry(enumEntry);
        }
        else {
            JvmClassName jvmClassName = bindingTrace.get(FQN, peekFromStack(classStack));
            assert PsiCodegenPredictor.checkPredictedNameFromPsi(bindingTrace, descriptor, jvmClassName);
            bindingTrace.record(FQN, descriptor, jvmClassName);
        }
    }

    @Override
    public void visitClassObject(JetClassObject classObject) {
        ClassDescriptor classDescriptor = bindingContext.get(CLASS, classObject.getObjectDeclaration());
        assert classDescriptor != null;

        JvmClassName name = JvmClassName.byInternalName(peekFromStack(nameStack) + JvmAbi.CLASS_OBJECT_SUFFIX);
        recordClosure(bindingTrace, classObject, classDescriptor, peekFromStack(classStack), name, false);

        classStack.push(classDescriptor);
        nameStack.push(name.getInternalName());
        super.visitClassObject(classObject);
        nameStack.pop();
        classStack.pop();
    }

    @Override
    public void visitObjectDeclaration(JetObjectDeclaration declaration) {
        if (declaration.getParent() instanceof JetObjectLiteralExpression || declaration.getParent() instanceof JetClassObject) {
            super.visitObjectDeclaration(declaration);
        }
        else {
            ClassDescriptor classDescriptor = bindingContext.get(CLASS, declaration);
            // working around a problem with shallow analysis
            if (classDescriptor == null) return;

            String name = getName(classDescriptor);
            recordClosure(bindingTrace, declaration, classDescriptor, peekFromStack(classStack), JvmClassName.byInternalName(name), false);

            classStack.push(classDescriptor);
            nameStack.push(name);
            super.visitObjectDeclaration(declaration);
            nameStack.pop();
            classStack.pop();
        }
    }

    @Override
    public void visitClass(JetClass klass) {
        ClassDescriptor classDescriptor = bindingContext.get(CLASS, klass);
        // working around a problem with shallow analysis
        if (classDescriptor == null) return;

        String name = getName(classDescriptor);
        recordClosure(bindingTrace, klass, classDescriptor, peekFromStack(classStack), JvmClassName.byInternalName(name), false);

        classStack.push(classDescriptor);
        nameStack.push(name);
        super.visitClass(klass);
        nameStack.pop();
        classStack.pop();
    }

    private String getName(ClassDescriptor classDescriptor) {
        String base = peekFromStack(nameStack);
        return DescriptorUtils.isTopLevelDeclaration(classDescriptor) ? base.isEmpty() ? classDescriptor.getName()
                        .getName() : base + '/' + classDescriptor.getName() : base + '$' + classDescriptor.getName();
    }

    @Override
    public void visitObjectLiteralExpression(JetObjectLiteralExpression expression) {
        ClassDescriptor classDescriptor = bindingContext.get(CLASS, expression.getObjectDeclaration());
        if (classDescriptor == null) {
            // working around a problem with shallow analysis
            super.visitObjectLiteralExpression(expression);
            return;
        }

        final String name = inventAnonymousClassName(expression.getObjectDeclaration());
        recordClosure(bindingTrace, expression.getObjectDeclaration(), classDescriptor, peekFromStack(classStack), JvmClassName.byInternalName(name), false);

        classStack.push(classDescriptor);
        //noinspection ConstantConditions
        nameStack.push(bindingContext.get(FQN, classDescriptor).getInternalName());
        super.visitObjectLiteralExpression(expression);
        nameStack.pop();
        classStack.pop();
    }

    @Override
    public void visitFunctionLiteralExpression(JetFunctionLiteralExpression expression) {
        FunctionDescriptor functionDescriptor =
                (FunctionDescriptor) bindingContext.get(DECLARATION_TO_DESCRIPTOR, expression);
        // working around a problem with shallow analysis
        if (functionDescriptor == null) return;

        String name = inventAnonymousClassName(expression);
        ClassDescriptor classDescriptor = recordClassForFunction(functionDescriptor);
        recordClosure(bindingTrace, expression, classDescriptor, peekFromStack(classStack), JvmClassName.byInternalName(name), true);

        classStack.push(classDescriptor);
        nameStack.push(name);
        super.visitFunctionLiteralExpression(expression);
        nameStack.pop();
        classStack.pop();
    }

    @Override
    public void visitProperty(JetProperty property) {
        nameStack.push(peekFromStack(nameStack) + '$' + property.getName());
        super.visitProperty(property);
        nameStack.pop();
    }

    @Override
    public void visitNamedFunction(JetNamedFunction function) {
        FunctionDescriptor functionDescriptor =
                (FunctionDescriptor) bindingContext.get(DECLARATION_TO_DESCRIPTOR, function);
        // working around a problem with shallow analysis
        if (functionDescriptor == null) return;
        DeclarationDescriptor containingDeclaration = functionDescriptor.getContainingDeclaration();
        if (containingDeclaration instanceof ClassDescriptor) {
            nameStack.push(peekFromStack(nameStack) + '$' + function.getName());
            super.visitNamedFunction(function);
            nameStack.pop();
        }
        else if (containingDeclaration instanceof NamespaceDescriptor) {
            String peek = peekFromStack(nameStack);
            FqName qualifiedName = ((NamespaceDescriptor) containingDeclaration).getFqName();
            String packageClassName = PackageClassUtils.getPackageClassName(qualifiedName);
            if (peek.isEmpty()) {
                peek = packageClassName;
            }
            else {
                peek += "/" + packageClassName;
            }
            nameStack.push(peek + '$' + function.getName());
            super.visitNamedFunction(function);
            nameStack.pop();
        }
        else {
            String name = inventAnonymousClassName(function);
            ClassDescriptor classDescriptor = recordClassForFunction(functionDescriptor);
            recordClosure(bindingTrace, function, classDescriptor, peekFromStack(classStack), JvmClassName.byInternalName(name), true);

            classStack.push(classDescriptor);
            nameStack.push(name);
            super.visitNamedFunction(function);
            nameStack.pop();
            classStack.pop();
        }
    }
}
