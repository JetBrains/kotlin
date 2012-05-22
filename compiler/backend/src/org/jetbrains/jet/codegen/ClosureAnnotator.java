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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.FqName;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.*;

/**
 * @author alex.tkachman
 */
public class ClosureAnnotator {
    private final Map<JetElement, JvmClassName> classNamesForAnonymousClasses = new HashMap<JetElement, JvmClassName>();
    private final Map<ClassDescriptor, JvmClassName> classNamesForClassDescriptor = new HashMap<ClassDescriptor, JvmClassName>();
    private final Map<String, Integer> anonymousSubclassesCount = new HashMap<String, Integer>();
    private final Map<FunctionDescriptor, ClassDescriptorImpl> classesForFunctions = new HashMap<FunctionDescriptor, ClassDescriptorImpl>();
    private final Map<DeclarationDescriptor,ClassDescriptor> enclosing = new HashMap<DeclarationDescriptor, ClassDescriptor>();

    private final MultiMap<FqName, JetFile> namespaceName2Files = MultiMap.create();

    private BindingContext bindingContext;
    private List<JetFile> files;

    @Inject
    public void setBindingContext(BindingContext bindingContext) {
        this.bindingContext = bindingContext;
    }

    @Inject
    public void setFiles(List<JetFile> files) {
        this.files = files;
    }

    @PostConstruct
    public void init() {
        mapFilesToNamespaces(files);
        prepareAnonymousClasses();
    }


    public ClassDescriptor classDescriptorForFunctionDescriptor(FunctionDescriptor funDescriptor, JvmClassName name) {
        ClassDescriptorImpl classDescriptor = classesForFunctions.get(funDescriptor);
        if(classDescriptor == null) {
            int arity = funDescriptor.getValueParameters().size();

            classDescriptor = new ClassDescriptorImpl(
                    funDescriptor,
                    Collections.<AnnotationDescriptor>emptyList(),
                    // TODO: internal name used as identifier
                    name.getInternalName()); // TODO:
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
            if (file.isScript()) {
                namespaceName2Files.putValue(FqName.ROOT, file);
            }
            else {
                FqName fqName = JetPsiUtil.getFQName(file);
                namespaceName2Files.putValue(fqName, file);
            }
        }
    }

    private void prepareAnonymousClasses() {
        MyJetVisitorVoid visitor = new MyJetVisitorVoid();
        for (Map.Entry<FqName, Collection<JetFile>> entry : namespaceName2Files.entrySet()) {
            for (JetFile jetFile : entry.getValue()) {
                jetFile.accept(visitor);
            }
        }
    }

    public JvmClassName classNameForAnonymousClass(JetElement expression) {
        if(expression instanceof JetObjectLiteralExpression) {
            JetObjectLiteralExpression jetObjectLiteralExpression = (JetObjectLiteralExpression) expression;
            expression = jetObjectLiteralExpression.getObjectDeclaration();
        }

        if(expression instanceof JetFunctionLiteralExpression) {
            JetFunctionLiteralExpression jetFunctionLiteralExpression = (JetFunctionLiteralExpression) expression;
            expression = jetFunctionLiteralExpression.getFunctionLiteral();
        }

        JvmClassName name = classNamesForAnonymousClasses.get(expression);
        assert name != null;
        return name;
    }

    public ClassDescriptor getEclosingClassDescriptor(ClassDescriptor descriptor) {
        return enclosing.get(descriptor);
    }

    public boolean hasThis0(ClassDescriptor classDescriptor) {
        if(DescriptorUtils.isClassObject(classDescriptor))
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

        private JvmClassName recordAnonymousClass(JetElement declaration) {
            JvmClassName name = classNamesForAnonymousClasses.get(declaration);
            assert name == null;

            String top = nameStack.peek();
            Integer cnt = anonymousSubclassesCount.get(top);
            if(cnt == null) {
                cnt = 0;
            }
            name = JvmClassName.byInternalName(top + "$" + (cnt + 1));
            classNamesForAnonymousClasses.put(declaration, name);
            anonymousSubclassesCount.put(top, cnt + 1);

            return name;
        }

        private JvmClassName recordClassObject(JetClassObject declaration) {
            JvmClassName name = classNamesForAnonymousClasses.get(declaration.getObjectDeclaration());
            assert name == null;

            name = JvmClassName.byInternalName(nameStack.peek() + JvmAbi.CLASS_OBJECT_SUFFIX);
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
            nameStack.push(JetPsiUtil.getFQName(file).getFqName().replace('.', '/'));
            file.acceptChildren(this);
            nameStack.pop();
        }

        @Override
        public void visitClassObject(JetClassObject classObject) {
            JvmClassName name = recordClassObject(classObject);
            ClassDescriptor classDescriptor = bindingContext.get(BindingContext.CLASS, classObject.getObjectDeclaration());
            recordEnclosing(classDescriptor);
            recordName(classDescriptor, name);
            classStack.push(classDescriptor);
            nameStack.push(name.getInternalName());
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
            JvmClassName name = recordAnonymousClass(expression.getObjectDeclaration());
            ClassDescriptor classDescriptor = bindingContext.get(BindingContext.CLASS, expression.getObjectDeclaration());
            recordName(classDescriptor, name);
            recordEnclosing(classDescriptor);
            classStack.push(classDescriptor);
            nameStack.push(classNameForClassDescriptor(classDescriptor).getInternalName());
            super.visitObjectLiteralExpression(expression);
            nameStack.pop();
            classStack.pop();
        }

        @Override
        public void visitFunctionLiteralExpression(JetFunctionLiteralExpression expression) {
            JvmClassName name = recordAnonymousClass(expression.getFunctionLiteral());
            FunctionDescriptor declarationDescriptor = (FunctionDescriptor) bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, expression);
            // working around a problem with shallow analysis
            if (declarationDescriptor == null) return;
            ClassDescriptor classDescriptor = classDescriptorForFunctionDescriptor(declarationDescriptor, name);
            recordName(classDescriptor, name);
            recordEnclosing(classDescriptor);
            classStack.push(classDescriptor);
            nameStack.push(classNameForClassDescriptor(classDescriptor).getInternalName());
            super.visitFunctionLiteralExpression(expression);
            nameStack.pop();
            classStack.pop();
        }

        // TODO: please insert either @NotNull or @Nullable here
        // stepan.koltsov@ 2012-04-08
        private void recordName(ClassDescriptor classDescriptor, @NotNull JvmClassName name) {
            JvmClassName old = classNamesForClassDescriptor.put(classDescriptor, name);
            if (old == null) {
                // TODO: fix this assertion
                // previosly here was incorrect assert that was ignored without -ea
                // stepan.koltsov@ 2012-04-08
                //throw new IllegalStateException("rewrite at key " + classDescriptor);
            }
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
                JvmClassName name = recordAnonymousClass(function);
                ClassDescriptor classDescriptor = classDescriptorForFunctionDescriptor(functionDescriptor, name);
                recordName(classDescriptor, name);
                recordEnclosing(classDescriptor);
                classStack.push(classDescriptor);
                nameStack.push(name.getInternalName());
                super.visitNamedFunction(function);
                nameStack.pop();
                classStack.pop();
            }
        }
    }

    public JvmClassName classNameForClassDescriptor(ClassDescriptor classDescriptor) {
        JvmClassName name = classNamesForClassDescriptor.get(classDescriptor);
        assert name != null;
        return name;
    }
}
