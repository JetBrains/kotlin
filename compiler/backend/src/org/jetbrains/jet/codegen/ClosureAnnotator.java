/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.codegen;

import com.intellij.util.containers.MultiMap;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;

import java.util.*;

/**
 * @author alex.tkachman
 */
public class ClosureAnnotator {
    private final Map<JetElement, String> classNamesForAnonymousClasses = new HashMap<JetElement, String>();
    private final Map<ClassDescriptor, String> classNamesForClassDescriptor = new HashMap<ClassDescriptor, String>();
    private final Map<String, Integer> anonymousSubclassesCount = new HashMap<String, Integer>();
    private final Map<FunctionDescriptor, ClassDescriptorImpl> classesForFunctions = new HashMap<FunctionDescriptor, ClassDescriptorImpl>();
    private final Map<DeclarationDescriptor,ClassDescriptor> enclosing = new HashMap<DeclarationDescriptor, ClassDescriptor>();

    private final MultiMap<String,JetFile> namespaceName2Files = MultiMap.create();
    private final BindingContext bindingContext;

    public ClosureAnnotator(BindingContext bindingContext, Collection<JetFile> files) {
        this.bindingContext = bindingContext;
        mapFilesToNamespaces(files);
        prepareAnonymousClasses();
    }

    public ClassDescriptor classDescriptorForFunctionDescriptor(FunctionDescriptor funDescriptor, String name) {
        ClassDescriptorImpl classDescriptor = classesForFunctions.get(funDescriptor);
        if(classDescriptor == null) {
            int arity = funDescriptor.getValueParameters().size();

            classDescriptor = new ClassDescriptorImpl(
                    funDescriptor,
                    Collections.<AnnotationDescriptor>emptyList(),
                    name);
            classDescriptor.initialize(
                    false,
                    Collections.<TypeParameterDescriptor>emptyList(),
                    Collections.singleton((funDescriptor.getReceiverParameter().exists() ? JetStandardClasses.getReceiverFunction(arity) : JetStandardClasses.getFunction(arity)).getDefaultType()), JetScope.EMPTY, Collections.<ConstructorDescriptor>emptySet(), null);
            classesForFunctions.put(funDescriptor, classDescriptor);
        }
        return classDescriptor;
    }

    private void mapFilesToNamespaces(Collection<JetFile> files) {
        for (JetFile file : files) {
            String fqName = JetPsiUtil.getFQName(file);
            namespaceName2Files.putValue(fqName, file);
        }
    }

    private void prepareAnonymousClasses() {
        MyJetVisitorVoid visitor = new MyJetVisitorVoid();
        for (Map.Entry<String,Collection<JetFile>> entry : namespaceName2Files.entrySet()) {
            for (JetFile jetFile : entry.getValue()) {
                jetFile.accept(visitor);
            }
        }
    }

    public String classNameForAnonymousClass(JetElement expression) {
        if(expression instanceof JetObjectLiteralExpression) {
            JetObjectLiteralExpression jetObjectLiteralExpression = (JetObjectLiteralExpression) expression;
            expression = jetObjectLiteralExpression.getObjectDeclaration();
        }

        if(expression instanceof JetFunctionLiteralExpression) {
            JetFunctionLiteralExpression jetFunctionLiteralExpression = (JetFunctionLiteralExpression) expression;
            expression = jetFunctionLiteralExpression.getFunctionLiteral();
        }

        String name = classNamesForAnonymousClasses.get(expression);
        assert name != null;
        return name;
    }

    public ClassDescriptor getEclosingClassDescriptor(ClassDescriptor descriptor) {
        return enclosing.get(descriptor);
    }

    public boolean hasThis0(ClassDescriptor classDescriptor) {
        if(CodegenUtil.isClassObject(classDescriptor))
            return false;

        ClassDescriptor other = enclosing.get(classDescriptor);
        return other != null;
    }

    private class MyJetVisitorVoid extends JetVisitorVoid {
        private LinkedList<ClassDescriptor> classStack = new LinkedList<ClassDescriptor>();
        private LinkedList<String> nameStack = new LinkedList<String>();

        private void recordEnclosing(ClassDescriptor classDescriptor) {
            if(classStack.size() > 0) {
                ClassDescriptor put = enclosing.put(classDescriptor, classStack.peek());
                assert put == null;
            }
        }

        private String recordAnonymousClass(JetElement declaration) {
            String name = classNamesForAnonymousClasses.get(declaration);
            assert name == null;

            String top = nameStack.peek();
            Integer cnt = anonymousSubclassesCount.get(top);
            if(cnt == null) {
                cnt = 0;
            }
            name = top + "$" + (cnt + 1);
            classNamesForAnonymousClasses.put(declaration, name);
            anonymousSubclassesCount.put(top, cnt + 1);

            return name;
        }

        private String recordClassObject(JetClassObject declaration) {
            String name = classNamesForAnonymousClasses.get(declaration.getObjectDeclaration());
            assert name == null;

            name = nameStack.peek() + JvmAbi.CLASS_OBJECT_SUFFIX;
            classNamesForAnonymousClasses.put(declaration.getObjectDeclaration(), name);

            return name;
        }

        @Override
        public void visitJetElement(JetElement element) {
            super.visitJetElement(element);
            element.acceptChildren(this);
        }

        @Override
        public void visitJetFile(JetFile file) {
            nameStack.push(JetPsiUtil.getFQName(file).replace('.', '/'));
            file.acceptChildren(this);
            nameStack.pop();
        }

        @Override
        public void visitClassObject(JetClassObject classObject) {
            String name = recordClassObject(classObject);
            ClassDescriptor classDescriptor = bindingContext.get(BindingContext.CLASS, classObject.getObjectDeclaration());
            recordEnclosing(classDescriptor);
            recordName(classDescriptor, name);
            classStack.push(classDescriptor);
            nameStack.push(name);
            super.visitClassObject(classObject);
            nameStack.pop();
            classStack.pop();
        }

        @Override
        public void visitObjectDeclaration(JetObjectDeclaration declaration) {
            if(declaration.getParent() instanceof JetObjectLiteralExpression || declaration.getParent() instanceof JetClassObject) {
                super.visitObjectDeclaration(declaration);
            }
            else {
                ClassDescriptor classDescriptor = bindingContext.get(BindingContext.CLASS, declaration);
                // working around a problem with shallow analysis
                if (classDescriptor == null) return;
                recordEnclosing(classDescriptor);
                classStack.push(classDescriptor);
                String base = nameStack.peek();
                if(classDescriptor.getContainingDeclaration() instanceof NamespaceDescriptor) {
                    nameStack.push(base.isEmpty() ? classDescriptor.getName() : base + '/' + classDescriptor.getName());
                }
                else
                    nameStack.push(base + '$' + classDescriptor.getName());
                super.visitObjectDeclaration(declaration);
                nameStack.pop();
                classStack.pop();
            }
        }

        @Override
        public void visitClass(JetClass klass) {
            ClassDescriptor classDescriptor = bindingContext.get(BindingContext.CLASS, klass);
            // working around a problem with shallow analysis
            if (classDescriptor == null) return;
            recordEnclosing(classDescriptor);
            classStack.push(classDescriptor);
            String base = nameStack.peek();
            if(classDescriptor.getContainingDeclaration() instanceof NamespaceDescriptor) {
                nameStack.push(base.isEmpty() ? classDescriptor.getName() : base + '/' + classDescriptor.getName());
            }
            else
                nameStack.push(base + '$' + classDescriptor.getName());
            super.visitClass(klass);
            nameStack.pop();
            classStack.pop();
        }

        @Override
        public void visitObjectLiteralExpression(JetObjectLiteralExpression expression) {
            String name = recordAnonymousClass(expression.getObjectDeclaration());
            ClassDescriptor classDescriptor = bindingContext.get(BindingContext.CLASS, expression.getObjectDeclaration());
            recordName(classDescriptor, name);
            recordEnclosing(classDescriptor);
            classStack.push(classDescriptor);
            nameStack.push(classNameForClassDescriptor(classDescriptor));
            super.visitObjectLiteralExpression(expression);
            nameStack.pop();
            classStack.pop();
        }

        @Override
        public void visitFunctionLiteralExpression(JetFunctionLiteralExpression expression) {
            String name = recordAnonymousClass(expression.getFunctionLiteral());
            FunctionDescriptor declarationDescriptor = (FunctionDescriptor) bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, expression);
            // working around a problem with shallow analysis
            if (declarationDescriptor == null) return;
            ClassDescriptor classDescriptor = classDescriptorForFunctionDescriptor(declarationDescriptor, name);
            recordName(classDescriptor, name);
            recordEnclosing(classDescriptor);
            classStack.push(classDescriptor);
            nameStack.push(classNameForClassDescriptor(classDescriptor));
            super.visitFunctionLiteralExpression(expression);
            nameStack.pop();
            classStack.pop();
        }

        private void recordName(ClassDescriptor classDescriptor, String name) {
            String old = classNamesForClassDescriptor.put(classDescriptor, name);
            assert old == null;
        }

        @Override
        public void visitProperty(JetProperty property) {
            nameStack.push(nameStack.peek() + '$' + property.getName());
            super.visitProperty(property);
            nameStack.pop();
        }

        @Override
        public void visitNamedFunction(JetNamedFunction function) {
            FunctionDescriptor functionDescriptor = (FunctionDescriptor) bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, function);
            // working around a problem with shallow analysis
            if (functionDescriptor == null) return;
            DeclarationDescriptor containingDeclaration = functionDescriptor.getContainingDeclaration();
            if (containingDeclaration instanceof ClassDescriptor) {
                nameStack.push(nameStack.peek() + '$' + function.getName());
                super.visitNamedFunction(function);
                nameStack.pop();
            }
            else if (containingDeclaration instanceof NamespaceDescriptor) {
                String peek = nameStack.peek();
                if(peek.isEmpty())
                    peek = "namespace";
                else
                    peek = peek + "/namespace";
                nameStack.push(peek + '$' + function.getName());
                super.visitNamedFunction(function);
                nameStack.pop();
            }
            else {
                String name = recordAnonymousClass(function);
                ClassDescriptor classDescriptor = classDescriptorForFunctionDescriptor(functionDescriptor, name);
                recordName(classDescriptor, name);
                recordEnclosing(classDescriptor);
                classStack.push(classDescriptor);
                nameStack.push(name);
                super.visitNamedFunction(function);
                nameStack.pop();
                classStack.pop();
            }
        }
    }

    public String classNameForClassDescriptor(ClassDescriptor classDescriptor) {
        String name = classNamesForClassDescriptor.get(classDescriptor);
        assert name != null;
        return name;
    }
}
