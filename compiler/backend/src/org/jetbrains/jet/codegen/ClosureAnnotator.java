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
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;

import javax.inject.Inject;
import java.util.*;

/**
 * @author alex.tkachman
 */
public class ClosureAnnotator {
    private final Map<ClassDescriptor, JvmClassName> classNamesForClassDescriptor = new HashMap<ClassDescriptor, JvmClassName>();
    private final Map<String, Integer> anonymousSubclassesCount = new HashMap<String, Integer>();
    private final Map<ScriptDescriptor, JvmClassName> classNameForScript = new HashMap<ScriptDescriptor, JvmClassName>();
    private final Map<ClassDescriptor, LocalClassClosureCodegen> localClassCodegenForClass =
            new HashMap<ClassDescriptor, LocalClassClosureCodegen>();
    private final Set<JvmClassName> scriptClassNames = new HashSet<JvmClassName>();
    private final Map<DeclarationDescriptor, ClassDescriptorImpl> classesForFunctions =
            new HashMap<DeclarationDescriptor, ClassDescriptorImpl>();
    private final Map<DeclarationDescriptor, ClassDescriptor> enclosing = new HashMap<DeclarationDescriptor, ClassDescriptor>();

    private final MultiMap<FqName, JetFile> namespaceName2MultiNamespaceFiles = MultiMap.create();
    private final MultiMap<FqName, JetFile> namespaceName2Files = MultiMap.create();

    private BindingContext bindingContext;
    private List<JetFile> files;
    private final Map<ClassDescriptor, Boolean> enumEntryNeedSubclass = new HashMap<ClassDescriptor, Boolean>();

    @Inject
    public void setBindingContext(BindingContext bindingContext) {
        this.bindingContext = bindingContext;
    }

    @Inject
    public void setFiles(List<JetFile> files) {
        this.files = files;
    }

    public void init() {
        mapFilesToNamespaces(files);
        prepareAnonymousClasses();
    }


    @NotNull
    public ClassDescriptor classDescriptorForFunctionDescriptor(FunctionDescriptor funDescriptor) {
        ClassDescriptorImpl classDescriptor = classesForFunctions.get(funDescriptor);
        if (classDescriptor == null) {
            int arity = funDescriptor.getValueParameters().size();

            classDescriptor = new ClassDescriptorImpl(
                    funDescriptor,
                    Collections.<AnnotationDescriptor>emptyList(),
                    Modality.FINAL,
                    Name.special("<closure>"));
            classDescriptor.initialize(
                    false,
                    Collections.<TypeParameterDescriptor>emptyList(),
                    Collections.singleton((funDescriptor.getReceiverParameter().exists()
                                           ? JetStandardClasses.getReceiverFunction(arity)
                                           : JetStandardClasses.getFunction(arity)).getDefaultType()), JetScope.EMPTY,
                    Collections.<ConstructorDescriptor>emptySet(), null);
            classesForFunctions.put(funDescriptor, classDescriptor);
        }
        return classDescriptor;
    }

    public void registerClassNameForScript(@NotNull ScriptDescriptor scriptDescriptor, @NotNull JvmClassName className) {
        JvmClassName oldName = classNameForScript.put(scriptDescriptor, className);
        if (oldName != null) {
            throw new IllegalStateException("Rewrite at key " + scriptDescriptor + " for name");
        }

        if (!scriptClassNames.add(className)) {
            throw new IllegalStateException("More than one script has class name " + className);
        }

        ClassDescriptorImpl classDescriptor = new ClassDescriptorImpl(
                scriptDescriptor,
                Collections.<AnnotationDescriptor>emptyList(),
                Modality.FINAL,
                Name.special("<script-" + className + ">"));
        recordName(classDescriptor, className);
        classDescriptor.initialize(
                false,
                Collections.<TypeParameterDescriptor>emptyList(),
                Collections.singletonList(JetStandardClasses.getAnyType()),
                JetScope.EMPTY,
                Collections.<ConstructorDescriptor>emptySet(),
                null);

        ClassDescriptorImpl oldDescriptor = classesForFunctions.put(scriptDescriptor, classDescriptor);
        if (oldDescriptor != null) {
            throw new IllegalStateException("Rewrite at key " + scriptDescriptor + " for class");
        }
    }

    public void registerClassNameForScript(@NotNull JetScript jetScript, @NotNull JvmClassName className) {
        ScriptDescriptor descriptor = bindingContext.get(BindingContext.SCRIPT, jetScript);
        if (descriptor == null) {
            throw new IllegalStateException("Descriptor is not found for PSI " + jetScript);
        }
        registerClassNameForScript(descriptor, className);
    }

    @NotNull
    public ClassDescriptor classDescriptorForScriptDescriptor(@NotNull ScriptDescriptor scriptDescriptor) {
        ClassDescriptorImpl classDescriptor = classesForFunctions.get(scriptDescriptor);
        if (classDescriptor == null) {
            throw new IllegalStateException("Class for script is not registered: " + scriptDescriptor);
        }
        return classDescriptor;
    }

    @NotNull
    public JvmClassName classNameForScriptPsi(@NotNull JetScript script) {
        ScriptDescriptor scriptDescriptor = bindingContext.get(BindingContext.SCRIPT, script);
        if (scriptDescriptor == null) {
            throw new IllegalStateException("Script descriptor not found by PSI " + script);
        }
        return classNameForScriptDescriptor(scriptDescriptor);
    }

    @NotNull
    public JvmClassName classNameForScriptDescriptor(@NotNull ScriptDescriptor scriptDescriptor) {
        return classNameForClassDescriptor(classDescriptorForScriptDescriptor(scriptDescriptor));
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

    public JvmClassName classNameForAnonymousClass(JetElement expression) {
        if (expression instanceof JetObjectLiteralExpression) {
            JetObjectLiteralExpression jetObjectLiteralExpression = (JetObjectLiteralExpression) expression;
            expression = jetObjectLiteralExpression.getObjectDeclaration();
        }

        ClassDescriptor descriptor = bindingContext.get(BindingContext.CLASS, expression);
        if (descriptor == null) {
            SimpleFunctionDescriptor functionDescriptor = bindingContext.get(BindingContext.FUNCTION, expression);
            assert functionDescriptor != null;
            descriptor = classDescriptorForFunctionDescriptor(functionDescriptor);
        }
        return classNameForClassDescriptor(descriptor);
    }

    public ClassDescriptor getEclosingClassDescriptor(ClassDescriptor descriptor) {
        return enclosing.get(descriptor);
    }

    public boolean hasThis0(ClassDescriptor classDescriptor) {
        if (DescriptorUtils.isClassObject(classDescriptor)) {
            return false;
        }
        if (classDescriptor.getKind() == ClassKind.ENUM_CLASS || classDescriptor.getKind() == ClassKind.ENUM_ENTRY) {
            return false;
        }

        ClassDescriptor other = enclosing.get(classDescriptor);
        return other != null;
    }

    public boolean enumEntryNeedSubclass(JetEnumEntry enumEntry) {
        ClassDescriptor descriptor = bindingContext.get(BindingContext.CLASS, enumEntry);
        return enumEntryNeedSubclass.get(descriptor);
    }

    public boolean enumEntryNeedSubclass(ClassDescriptor enumEntry) {
        Boolean aBoolean = enumEntryNeedSubclass.get(enumEntry);
        return aBoolean != null && aBoolean;
    }

    public void recordLocalClass(ClassDescriptor descriptor, LocalClassClosureCodegen codegen) {
        LocalClassClosureCodegen put = localClassCodegenForClass.put(descriptor, codegen);
        assert put == null;
    }

    private class MyJetVisitorVoid extends JetVisitorVoid {
        private final LinkedList<ClassDescriptor> classStack = new LinkedList<ClassDescriptor>();
        private final LinkedList<String> nameStack = new LinkedList<String>();

        private void recordEnclosing(ClassDescriptor classDescriptor) {
            if (classStack.size() > 0) {
                ClassDescriptor put = enclosing.put(classDescriptor, classStack.peek());
                assert put == null;
            }
        }

        private JvmClassName recordAnonymousClass(JetElement declaration) {
            String top = nameStack.peek();
            Integer cnt = anonymousSubclassesCount.get(top);
            if (cnt == null) {
                cnt = 0;
            }
            JvmClassName name = JvmClassName.byInternalName(top + "$" + (cnt + 1));
            ClassDescriptor descriptor = bindingContext.get(BindingContext.CLASS, declaration);
            if (descriptor == null) {
                if (declaration instanceof JetFunctionLiteralExpression || declaration instanceof JetNamedFunction) {
                    DeclarationDescriptor declarationDescriptor = bindingContext.get(BindingContext.FUNCTION, declaration);
                    assert declarationDescriptor instanceof FunctionDescriptor;
                    descriptor = classDescriptorForFunctionDescriptor((FunctionDescriptor) declarationDescriptor);
                }
                else if (declaration instanceof JetObjectLiteralExpression) {
                    descriptor =
                            bindingContext.get(BindingContext.CLASS, ((JetObjectLiteralExpression) declaration).getObjectDeclaration());
                    assert descriptor != null;
                }
                else {
                    throw new IllegalStateException(
                            "Class-less declaration which is not JetFunctionLiteralExpression|JetNamedFunction|JetObjectLiteralExpression : " +
                            declaration.getClass().getName());
                }
            }
            recordName(descriptor, name);
            anonymousSubclassesCount.put(top, cnt + 1);

            return name;
        }

        private JvmClassName recordClassObject(JetClassObject declaration) {
            JvmClassName name = JvmClassName.byInternalName(nameStack.peek() + JvmAbi.CLASS_OBJECT_SUFFIX);
            ClassDescriptor classDescriptor = bindingContext.get(BindingContext.CLASS, declaration.getObjectDeclaration());
            assert classDescriptor != null;
            recordName(classDescriptor, name);
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
                nameStack.push(classNameForScriptPsi(file.getScript()).getInternalName());
            }
            else {
                nameStack.push(JetPsiUtil.getFQName(file).getFqName().replace('.', '/'));
            }
            file.acceptChildren(this);
            nameStack.pop();
        }

        @Override
        public void visitEnumEntry(JetEnumEntry enumEntry) {
            ClassDescriptor descriptor = bindingContext.get(BindingContext.CLASS, enumEntry);
            enumEntryNeedSubclass.put(descriptor, !enumEntry.getDeclarations().isEmpty());
            super.visitEnumEntry(enumEntry);
        }

        @Override
        public void visitClassObject(JetClassObject classObject) {
            JvmClassName name = recordClassObject(classObject);
            ClassDescriptor classDescriptor = bindingContext.get(BindingContext.CLASS, classObject.getObjectDeclaration());
            assert classDescriptor != null;
            recordEnclosing(classDescriptor);
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
                ClassDescriptor classDescriptor = bindingContext.get(BindingContext.CLASS, declaration);
                // working around a problem with shallow analysis
                if (classDescriptor == null) return;
                recordEnclosing(classDescriptor);
                classStack.push(classDescriptor);
                String base = nameStack.peek();
                if (classDescriptor.getContainingDeclaration() instanceof NamespaceDescriptor) {
                    nameStack.push(base.isEmpty() ? classDescriptor.getName().getName() : base + '/' + classDescriptor.getName());
                }
                else {
                    nameStack.push(base + '$' + classDescriptor.getName());
                }
                recordName(classDescriptor, JvmClassName.byInternalName(nameStack.peek()));
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
            if (classDescriptor.getContainingDeclaration() instanceof NamespaceDescriptor) {
                nameStack.push(base.isEmpty() ? classDescriptor.getName().getName() : base + '/' + classDescriptor.getName());
            }
            else {
                nameStack.push(base + '$' + classDescriptor.getName());
            }
            recordName(classDescriptor, JvmClassName.byInternalName(nameStack.peek()));
            super.visitClass(klass);
            nameStack.pop();
            classStack.pop();
        }

        @Override
        public void visitObjectLiteralExpression(JetObjectLiteralExpression expression) {
            ClassDescriptor classDescriptor = bindingContext.get(BindingContext.CLASS, expression.getObjectDeclaration());
            if (classDescriptor == null) {
                // working around a problem with shallow analysis
                super.visitObjectLiteralExpression(expression);
                return;
            }
            recordAnonymousClass(expression.getObjectDeclaration());
            recordEnclosing(classDescriptor);
            classStack.push(classDescriptor);
            nameStack.push(classNameForClassDescriptor(classDescriptor).getInternalName());
            super.visitObjectLiteralExpression(expression);
            nameStack.pop();
            classStack.pop();
        }

        @Override
        public void visitFunctionLiteralExpression(JetFunctionLiteralExpression expression) {
            FunctionDescriptor functionDescriptor =
                    (FunctionDescriptor) bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, expression);
            // working around a problem with shallow analysis
            if (functionDescriptor == null) return;
            JvmClassName name = recordAnonymousClass(expression);
            ClassDescriptor classDescriptor = classDescriptorForFunctionDescriptor(functionDescriptor);
            recordEnclosing(classDescriptor);
            classStack.push(classDescriptor);
            nameStack.push(name.getInternalName());
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
                    (FunctionDescriptor) bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, function);
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
                JvmClassName name = recordAnonymousClass(function);
                ClassDescriptor classDescriptor = classDescriptorForFunctionDescriptor(functionDescriptor);
                recordEnclosing(classDescriptor);
                classStack.push(classDescriptor);
                nameStack.push(name.getInternalName());
                super.visitNamedFunction(function);
                nameStack.pop();
                classStack.pop();
            }
        }
    }

    private void recordName(@NotNull ClassDescriptor classDescriptor, @NotNull JvmClassName name) {
        JvmClassName old = classNamesForClassDescriptor.put(classDescriptor, name);
        if (old != null) {
            throw new IllegalStateException("rewrite at key " + classDescriptor);
        }
    }

    public JvmClassName classNameForClassDescriptor(@NotNull ClassDescriptor classDescriptor) {
        return classNamesForClassDescriptor.get(classDescriptor);
    }
}
