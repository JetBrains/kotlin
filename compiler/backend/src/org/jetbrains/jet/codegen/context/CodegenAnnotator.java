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

package org.jetbrains.jet.codegen.context;

import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;

import javax.inject.Inject;
import java.util.*;

import static org.jetbrains.jet.codegen.context.CodegenBinding.*;
import static org.jetbrains.jet.lang.resolve.BindingContext.*;

/**
 * @author alex.tkachman
 */
public class CodegenAnnotator {
    private final MultiMap<FqName, JetFile> namespaceName2MultiNamespaceFiles = MultiMap.create();
    private final MultiMap<FqName, JetFile> namespaceName2Files = MultiMap.create();

    private BindingTrace bindingTrace;
    private BindingContext bindingContext;
    private List<JetFile> files;

    public BindingTrace getBindingTrace() {
        return bindingTrace;
    }

    @Inject
    public void setBindingTrace(BindingTrace bindingTrace) {
        this.bindingTrace = bindingTrace;
        bindingContext = bindingTrace.getBindingContext();
    }

    @Inject
    public void setFiles(List<JetFile> files) {
        this.files = files;
    }

    public void init() {
        mapFilesToNamespaces(files);
        prepareAnonymousClasses();
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
                Collections.singleton((funDescriptor.getReceiverParameter().exists()
                                       ? JetStandardClasses.getReceiverFunction(arity)
                                       : JetStandardClasses.getFunction(arity)).getDefaultType()), JetScope.EMPTY,
                Collections.<ConstructorDescriptor>emptySet(), null);

        bindingTrace.record(CLASS_FOR_FUNCTION, funDescriptor, classDescriptor);
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

            Collection<JetFile> namespaceFiles = new ArrayList<JetFile>();
            for (JetFile jetFile : entry.getValue()) {
                Collection<JetDeclaration> fileFunctions = new ArrayList<JetDeclaration>();
                for (JetDeclaration declaration : jetFile.getDeclarations()) {
                    if (declaration instanceof JetNamedFunction) {
                        fileFunctions.add(declaration);
                    }
                }

                if (fileFunctions.size() > 0) {
                    namespaceFiles.add(jetFile);
                }
            }

            if (namespaceFiles.size() > 1) {
                for (JetFile namespaceFile : namespaceFiles) {
                    namespaceName2MultiNamespaceFiles.putValue(entry.getKey(), namespaceFile);
                }
            }
        }
    }

    public boolean isMultiFileNamespace(FqName fqName) {
        return namespaceName2MultiNamespaceFiles.get(fqName).size() > 0;
    }

    private class MyJetVisitorVoid extends JetVisitorVoid {
        private final Map<String, Integer> anonymousSubclassesCount = new HashMap<String, Integer>();

        private final LinkedList<ClassDescriptor> classStack = new LinkedList<ClassDescriptor>();
        private final LinkedList<String> nameStack = new LinkedList<String>();

        private String inventAnonymousClassName(JetElement declaration) {
            String top = nameStack.peek();
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
                nameStack.push(JetPsiUtil.getFQName(file).getFqName().replace('.', '/'));
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

                classStack.push(descriptor);
                nameStack.push(nameStack.peek() + "$" + descriptor.getName().getName());
                super.visitEnumEntry(enumEntry);
                nameStack.pop();
                classStack.pop();
            }
            else {
                super.visitEnumEntry(enumEntry);
            }
        }

        @Override
        public void visitClassObject(JetClassObject classObject) {
            ClassDescriptor classDescriptor = bindingContext.get(CLASS, classObject.getObjectDeclaration());
            assert classDescriptor != null;

            JvmClassName name = JvmClassName.byInternalName(nameStack.peek() + JvmAbi.CLASS_OBJECT_SUFFIX);
            recordClosure(bindingTrace, classObject, classDescriptor, classStack.peek(), name, false);

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
                recordClosure(bindingTrace, declaration, classDescriptor, classStack.peek(), JvmClassName.byInternalName(name), false);

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
            recordClosure(bindingTrace, klass, classDescriptor, classStack.peek(), JvmClassName.byInternalName(name), false);

            classStack.push(classDescriptor);
            nameStack.push(name);
            super.visitClass(klass);
            nameStack.pop();
            classStack.pop();
        }

        private String getName(ClassDescriptor classDescriptor) {
            String base = nameStack.peek();
            return classDescriptor.getContainingDeclaration() instanceof NamespaceDescriptor ? base.isEmpty() ? classDescriptor.getName()
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
            recordClosure(bindingTrace, expression.getObjectDeclaration(), classDescriptor, classStack.peek(), JvmClassName.byInternalName(name), false);

            classStack.push(classDescriptor);
            nameStack.push(classNameForClassDescriptor(bindingContext, classDescriptor).getInternalName());
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
            recordClosure(bindingTrace, expression, classDescriptor, classStack.peek(), JvmClassName.byInternalName(name), true);

            classStack.push(classDescriptor);
            nameStack.push(name);
            super.visitFunctionLiteralExpression(expression);
            nameStack.pop();
            classStack.pop();
        }

        @Override
        public void visitProperty(JetProperty property) {
            nameStack.push(nameStack.peek() + '$' + property.getName());
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
                nameStack.push(nameStack.peek() + '$' + function.getName());
                super.visitNamedFunction(function);
                nameStack.pop();
            }
            else if (containingDeclaration instanceof NamespaceDescriptor) {
                String peek = nameStack.peek();
                if (peek.isEmpty()) {
                    peek = "namespace";
                }
                else {
                    peek += "/namespace";
                }
                nameStack.push(peek + '$' + function.getName());
                super.visitNamedFunction(function);
                nameStack.pop();
            }
            else {
                String name = inventAnonymousClassName(function);
                ClassDescriptor classDescriptor = recordClassForFunction(functionDescriptor);
                recordClosure(bindingTrace, function, classDescriptor, classStack.peek(), JvmClassName.byInternalName(name), true);

                classStack.push(classDescriptor);
                nameStack.push(name);
                super.visitNamedFunction(function);
                nameStack.pop();
                classStack.pop();
            }
        }
    }
}
