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

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.ClassDescriptorImpl;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.util.slicedmap.Slices;
import org.jetbrains.jet.util.slicedmap.WritableSlice;

import java.util.*;

import static org.jetbrains.jet.codegen.CodegenUtil.isInterface;
import static org.jetbrains.jet.lang.resolve.BindingContext.*;
import static org.jetbrains.jet.lang.resolve.BindingContextUtils.descriptorToDeclaration;

public class CodegenBinding {
    public static final WritableSlice<ClassDescriptor, MutableClosure> CLOSURE = Slices.createSimpleSlice();

    public static final WritableSlice<DeclarationDescriptor, ClassDescriptor> CLASS_FOR_FUNCTION = Slices.createSimpleSlice();

    public static final WritableSlice<DeclarationDescriptor, JvmClassName> FQN = Slices.createSimpleSlice();

    public static final WritableSlice<JvmClassName, Boolean> SCRIPT_NAMES = Slices.createSimpleSetSlice();

    public static final WritableSlice<ClassDescriptor, Boolean> ENUM_ENTRY_CLASS_NEED_SUBCLASS = Slices.createSimpleSetSlice();

    private CodegenBinding() {
    }

    public static void initTrace(BindingTrace bindingTrace, Collection<JetFile> files) {
        CodegenAnnotatingVisitor visitor = new CodegenAnnotatingVisitor(bindingTrace);
        for (JetFile file : allFilesInNamespaces(bindingTrace.getBindingContext(), files)) {
            file.accept(visitor);
        }
    }

    public static boolean enumEntryNeedSubclass(BindingContext bindingContext, JetEnumEntry enumEntry) {
        return enumEntryNeedSubclass(bindingContext, bindingContext.get(CLASS, enumEntry));
    }

    public static boolean enumEntryNeedSubclass(BindingContext bindingContext, ClassDescriptor classDescriptor) {
        return Boolean.TRUE.equals(bindingContext.get(ENUM_ENTRY_CLASS_NEED_SUBCLASS, classDescriptor));
    }

    @NotNull
    public static JvmClassName classNameForScriptDescriptor(BindingContext bindingContext, @NotNull ScriptDescriptor scriptDescriptor) {
        final ClassDescriptor classDescriptor = bindingContext.get(CLASS_FOR_FUNCTION, scriptDescriptor);
        //noinspection ConstantConditions
        return bindingContext.get(FQN, classDescriptor);
    }

    @NotNull
    public static JvmClassName classNameForScriptPsi(BindingContext bindingContext, @NotNull JetScript script) {
        ScriptDescriptor scriptDescriptor = bindingContext.get(SCRIPT, script);
        if (scriptDescriptor == null) {
            throw new IllegalStateException("Script descriptor not found by PSI " + script);
        }
        return classNameForScriptDescriptor(bindingContext, scriptDescriptor);
    }

    public static ClassDescriptor enclosingClassDescriptor(BindingContext bindingContext, ClassDescriptor descriptor) {
        final CalculatedClosure closure = bindingContext.get(CLOSURE, descriptor);
        return closure == null ? null : closure.getEnclosingClass();
    }

    public static JvmClassName classNameForAnonymousClass(BindingContext bindingContext, JetElement expression) {
        if (expression instanceof JetObjectLiteralExpression) {
            JetObjectLiteralExpression jetObjectLiteralExpression = (JetObjectLiteralExpression) expression;
            expression = jetObjectLiteralExpression.getObjectDeclaration();
        }

        ClassDescriptor descriptor = bindingContext.get(CLASS, expression);
        if (descriptor == null) {
            SimpleFunctionDescriptor functionDescriptor = bindingContext.get(FUNCTION, expression);
            assert functionDescriptor != null;
            descriptor = bindingContext.get(CLASS_FOR_FUNCTION, functionDescriptor);
        }
        return bindingContext.get(FQN, descriptor);
    }

    public static void registerClassNameForScript(
            BindingTrace bindingTrace,
            @NotNull ScriptDescriptor scriptDescriptor,
            @NotNull JvmClassName className
    ) {
        bindingTrace.record(SCRIPT_NAMES, className);

        ClassDescriptorImpl classDescriptor = new ClassDescriptorImpl(
                scriptDescriptor,
                Collections.<AnnotationDescriptor>emptyList(),
                Modality.FINAL,
                Name.special("<script-" + className + ">"));
        classDescriptor.initialize(
                false,
                Collections.<TypeParameterDescriptor>emptyList(),
                Collections.singletonList(KotlinBuiltIns.getInstance().getAnyType()),
                JetScope.EMPTY,
                Collections.<ConstructorDescriptor>emptySet(),
                null,
                false);

        recordClosure(bindingTrace, null, classDescriptor, null, className, false);

        assert PsiCodegenPredictor.checkPredictedClassNameForFun(bindingTrace.getBindingContext(), scriptDescriptor, classDescriptor);
        bindingTrace.record(CLASS_FOR_FUNCTION, scriptDescriptor, classDescriptor);
    }

    public static boolean canHaveOuter(BindingContext bindingContext, @NotNull ClassDescriptor classDescriptor) {
        if (isSingleton(bindingContext, classDescriptor)) {
            return false;
        }

        ClassDescriptor enclosing = enclosingClassDescriptor(bindingContext, classDescriptor);
        if (enclosing == null) {
            return false;
        }

        ClassKind kind = classDescriptor.getKind();
        if (kind == ClassKind.CLASS) {
            return classDescriptor.isInner() || !(classDescriptor.getContainingDeclaration() instanceof ClassDescriptor);
        }
        else if (kind == ClassKind.OBJECT) {
            return !isSingleton(bindingContext, enclosing);
        }
        else {
            return false;
        }
    }

    public static boolean isSingleton(BindingContext bindingContext, @NotNull ClassDescriptor classDescriptor) {
        final ClassKind kind = classDescriptor.getKind();
        if (kind == ClassKind.CLASS_OBJECT) {
            return true;
        }

        if (kind == ClassKind.OBJECT && isObjectDeclaration(bindingContext, classDescriptor)) {
            return true;
        }

        if (kind == ClassKind.ENUM_ENTRY) {
            return true;
        }

        return false;
    }

    static void recordClosure(
            BindingTrace bindingTrace,
            @Nullable JetElement element,
            ClassDescriptor classDescriptor,
            @Nullable ClassDescriptor enclosing,
            JvmClassName name,
            boolean functionLiteral
    ) {
        final JetDelegatorToSuperCall superCall = findSuperCall(bindingTrace.getBindingContext(), element);

        CallableDescriptor enclosingReceiver = null;
        if (classDescriptor.getContainingDeclaration() instanceof CallableDescriptor) {
            enclosingReceiver = (CallableDescriptor) classDescriptor.getContainingDeclaration();
            enclosingReceiver = enclosingReceiver instanceof PropertyAccessorDescriptor
                                ? ((PropertyAccessorDescriptor) enclosingReceiver).getCorrespondingProperty()
                                : enclosingReceiver;

            if (enclosingReceiver.getReceiverParameter() == null) {
                enclosingReceiver = null;
            }
        }

        final MutableClosure closure = new MutableClosure(superCall, enclosing, enclosingReceiver);

        assert PsiCodegenPredictor.checkPredictedNameFromPsi(bindingTrace, classDescriptor, name);
        bindingTrace.record(FQN, classDescriptor, name);
        bindingTrace.record(CLOSURE, classDescriptor, closure);

        // TODO: this is temporary before we have proper inner classes
        if (canHaveOuter(bindingTrace.getBindingContext(), classDescriptor) && !functionLiteral) {
            closure.setCaptureThis();
        }
    }

    public static void registerClassNameForScript(BindingTrace bindingTrace, @NotNull JetScript jetScript, @NotNull JvmClassName className) {
        ScriptDescriptor descriptor = bindingTrace.getBindingContext().get(SCRIPT, jetScript);
        if (descriptor == null) {
            throw new IllegalStateException("Descriptor is not found for PSI " + jetScript);
        }
        registerClassNameForScript(bindingTrace, descriptor, className);
    }

    @NotNull public static Collection<JetFile> allFilesInNamespaces(BindingContext bindingContext, Collection<JetFile> files) {
        // todo: we use Set and add given files but ignoring other scripts because something non-clear kept in binding
        // for scripts especially in case of REPL

        final HashSet<FqName> names = new HashSet<FqName>();
        for (JetFile file : files) {
            if (!file.isScript()) {
                names.add(JetPsiUtil.getFQName(file));
            }
        }

        final HashSet<JetFile> answer = new HashSet<JetFile>();
        answer.addAll(files);

        for (FqName name : names) {
            final NamespaceDescriptor namespaceDescriptor = bindingContext.get(BindingContext.FQNAME_TO_NAMESPACE_DESCRIPTOR, name);
            final Collection<JetFile> jetFiles = bindingContext.get(NAMESPACE_TO_FILES, namespaceDescriptor);
            if (jetFiles != null)
                answer.addAll(jetFiles);
        }

        List<JetFile> sortedAnswer = new ArrayList<JetFile>(answer);
        Collections.sort(sortedAnswer, new Comparator<JetFile>() {
            @NotNull
            private String path(JetFile file) {
                VirtualFile virtualFile = file.getVirtualFile();
                assert virtualFile != null : "VirtualFile is null for JetFile: " + file.getName();
                return virtualFile.getPath();
            }

            @Override
            public int compare(JetFile first, JetFile second) {
                return path(first).compareTo(path(second));
            }
        });

        return sortedAnswer;
    }

    public static boolean isMultiFileNamespace(BindingContext bindingContext, FqName fqName) {
        final NamespaceDescriptor namespaceDescriptor = bindingContext.get(BindingContext.FQNAME_TO_NAMESPACE_DESCRIPTOR, fqName);
        final Collection<JetFile> jetFiles = bindingContext.get(NAMESPACE_TO_FILES, namespaceDescriptor);
        return jetFiles != null && jetFiles.size() > 1;
    }

    public static boolean isObjectLiteral(BindingContext bindingContext, ClassDescriptor declaration) {
        PsiElement psiElement = descriptorToDeclaration(bindingContext, declaration);
        if (psiElement instanceof JetObjectDeclaration && ((JetObjectDeclaration) psiElement).isObjectLiteral()) {
            return true;
        }
        return false;
    }

    public static boolean isObjectDeclaration(BindingContext bindingContext, ClassDescriptor declaration) {
        PsiElement psiElement = descriptorToDeclaration(bindingContext, declaration);
        if (psiElement instanceof JetObjectDeclaration && !((JetObjectDeclaration) psiElement).isObjectLiteral()) {
            return true;
        }
        return false;
    }

    public static boolean isLocalFun(BindingContext bindingContext, DeclarationDescriptor fd) {
        PsiElement psiElement = descriptorToDeclaration(bindingContext, fd);
        if (psiElement instanceof JetNamedFunction && psiElement.getParent() instanceof JetBlockExpression) {
            return true;
        }
        return false;
    }

    public static boolean isLocalNamedFun(BindingContext bindingContext, DeclarationDescriptor fd) {
        PsiElement psiElement = descriptorToDeclaration(bindingContext, fd);
        if (psiElement instanceof JetNamedFunction) {
            final DeclarationDescriptor declaration = fd.getContainingDeclaration();
            return declaration instanceof FunctionDescriptor || declaration instanceof PropertyDescriptor;
        }
        return false;
    }

    @NotNull
    public static JvmClassName getJvmInternalName(BindingTrace bindingTrace, @NotNull DeclarationDescriptor descriptor) {
        descriptor = descriptor.getOriginal();
        JvmClassName name = bindingTrace.getBindingContext().get(FQN, descriptor);
        if (name != null) {
            return name;
        }

        name = JvmClassName.byInternalName(getJvmInternalFQNameImpl(bindingTrace, descriptor));

        assert PsiCodegenPredictor.checkPredictedNameFromPsi(bindingTrace, descriptor, name);
        bindingTrace.record(FQN, descriptor, name);
        return name;
    }

    private static String getJvmInternalFQNameImpl(BindingTrace bindingTrace, DeclarationDescriptor descriptor) {
        if (descriptor instanceof FunctionDescriptor) {
            throw new IllegalStateException("requested fq name for function: " + descriptor);
        }

        if (descriptor.getContainingDeclaration() instanceof ModuleDescriptor || descriptor instanceof ScriptDescriptor) {
            return "";
        }

        if (descriptor instanceof ModuleDescriptor) {
            throw new IllegalStateException("missed something");
        }

        if (descriptor instanceof ClassDescriptor) {
            ClassDescriptor klass = (ClassDescriptor) descriptor;
            if (klass.getKind() == ClassKind.OBJECT || klass.getKind() == ClassKind.CLASS_OBJECT) {
                if (klass.getContainingDeclaration() instanceof ClassDescriptor) {
                    ClassDescriptor containingKlass = (ClassDescriptor) klass.getContainingDeclaration();
                    if (containingKlass.getKind() == ClassKind.ENUM_CLASS) {
                        return getJvmInternalName(bindingTrace, containingKlass).getInternalName();
                    }
                    else {
                        return getJvmInternalName(bindingTrace, containingKlass).getInternalName() + JvmAbi.CLASS_OBJECT_SUFFIX;
                    }
                }
            }

            JvmClassName name = bindingTrace.getBindingContext().get(FQN, descriptor);
            if (name != null) {
                return name.getInternalName();
            }
        }

        DeclarationDescriptor container = descriptor.getContainingDeclaration();

        if (container == null) {
            throw new IllegalStateException("descriptor has no container: " + descriptor);
        }

        Name name = descriptor.getName();

        String baseName = getJvmInternalName(bindingTrace, container).getInternalName();
        if (!baseName.isEmpty()) {
            return baseName + (container instanceof NamespaceDescriptor ? "/" : "$") + name.getIdentifier();
        }

        return name.getIdentifier();
    }

    public static boolean isVarCapturedInClosure(BindingContext bindingContext, DeclarationDescriptor descriptor) {
        if (!(descriptor instanceof VariableDescriptor) || descriptor instanceof PropertyDescriptor) return false;
        VariableDescriptor variableDescriptor = (VariableDescriptor) descriptor;
        return Boolean.TRUE.equals(bindingContext.get(CAPTURED_IN_CLOSURE, variableDescriptor)) &&
               variableDescriptor.isVar();
    }

    public static boolean hasThis0(BindingContext bindingContext, ClassDescriptor classDescriptor) {
        //noinspection SuspiciousMethodCalls
        final CalculatedClosure closure = bindingContext.get(CLOSURE, classDescriptor);
        return closure != null && closure.getCaptureThis() != null;
    }

    private static JetDelegatorToSuperCall findSuperCall(
            BindingContext bindingContext,
            JetElement classOrObject
    ) {
        if (!(classOrObject instanceof JetClassOrObject)) {
            return null;
        }

        if (classOrObject instanceof JetClass && ((JetClass) classOrObject).isTrait()) {
            return null;
        }
        for (JetDelegationSpecifier specifier : ((JetClassOrObject) classOrObject).getDelegationSpecifiers()) {
            if (specifier instanceof JetDelegatorToSuperCall) {
                JetType superType = bindingContext.get(TYPE, specifier.getTypeReference());
                assert superType != null;
                ClassDescriptor superClassDescriptor = (ClassDescriptor) superType.getConstructor().getDeclarationDescriptor();
                assert superClassDescriptor != null;
                if (!isInterface(superClassDescriptor)) {
                    return (JetDelegatorToSuperCall) specifier;
                }
            }
        }

        return null;
    }
}
