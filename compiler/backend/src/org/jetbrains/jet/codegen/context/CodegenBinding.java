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

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.CodegenUtil;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;
import org.jetbrains.jet.util.slicedmap.*;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import static org.jetbrains.jet.lang.resolve.BindingContext.*;

/**
 * @author alex.tkachman
 */
public class CodegenBinding {
    public static final WritableSlice<ClassDescriptor, MutableClosure> CLOSURE = Slices.createSimpleSlice();

    public static final WritableSlice<DeclarationDescriptor, ClassDescriptor> CLASS_FOR_FUNCTION = Slices.createSimpleSlice();

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
        final CalculatedClosure closure = bindingContext.get(CLOSURE, classDescriptor);
        assert closure != null;
        return closure.getClassName();
    }

    @NotNull
    public static JvmClassName classNameForScriptPsi(BindingContext bindingContext, @NotNull JetScript script) {
        ScriptDescriptor scriptDescriptor = bindingContext.get(SCRIPT, script);
        if (scriptDescriptor == null) {
            throw new IllegalStateException("Script descriptor not found by PSI " + script);
        }
        return classNameForScriptDescriptor(bindingContext, scriptDescriptor);
    }

    public static ClassDescriptor eclosingClassDescriptor(BindingContext bindingContext, ClassDescriptor descriptor) {
        final CalculatedClosure closure = bindingContext.get(CLOSURE, descriptor);
        return closure == null ? null : closure.getEnclosingClass();
    }

    public static JvmClassName classNameForClassDescriptor(BindingContext bindingContext, ClassDescriptor descriptor) {
        final CalculatedClosure closure = bindingContext.get(CLOSURE, descriptor);
        return closure == null ? null : closure.getClassName();
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
        return classNameForClassDescriptor(bindingContext, descriptor);
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
                Collections.singletonList(JetStandardClasses.getAnyType()),
                JetScope.EMPTY,
                Collections.<ConstructorDescriptor>emptySet(),
                null);

        recordClosure(bindingTrace, null, classDescriptor, null, className, false);

        bindingTrace.record(CLASS_FOR_FUNCTION, scriptDescriptor, classDescriptor);
    }

    private static boolean canHaveOuter(BindingContext bindingContext, ClassDescriptor classDescriptor) {
        if (DescriptorUtils.isClassObject(classDescriptor)) {
            return false;
        }
        if (classDescriptor.getKind() == ClassKind.ENUM_CLASS || classDescriptor.getKind() == ClassKind.ENUM_ENTRY) {
            return false;
        }

        return eclosingClassDescriptor(bindingContext, classDescriptor) != null;
    }

    static void recordClosure(
            BindingTrace bindingTrace,
            @Nullable JetElement element,
            ClassDescriptor classDescriptor,
            @Nullable ClassDescriptor enclosing,
            JvmClassName name,
            boolean functionLiteral
    ) {
        final JetDelegatorToSuperCall superCall = CodegenUtil.findSuperCall(element, bindingTrace.getBindingContext());

        CallableDescriptor enclosingReceiver = null;
        if (classDescriptor.getContainingDeclaration() instanceof CallableDescriptor) {
            enclosingReceiver = (CallableDescriptor) classDescriptor.getContainingDeclaration();
            enclosingReceiver = enclosingReceiver instanceof PropertyAccessorDescriptor
                                ? ((PropertyAccessorDescriptor) enclosingReceiver).getCorrespondingProperty()
                                : enclosingReceiver;

            if (!enclosingReceiver.getReceiverParameter().exists()) {
                enclosingReceiver = null;
            }
        }

        final MutableClosure closure = new MutableClosure(superCall, enclosing, name, enclosingReceiver);

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
            if(jetFiles != null)
                answer.addAll(jetFiles);
        }
        return answer;
    }

    public static boolean isMultiFileNamespace(BindingContext bindingContext, FqName fqName) {
        final NamespaceDescriptor namespaceDescriptor = bindingContext.get(BindingContext.FQNAME_TO_NAMESPACE_DESCRIPTOR, fqName);
        final Collection<JetFile> jetFiles = bindingContext.get(NAMESPACE_TO_FILES, namespaceDescriptor);
        return jetFiles != null && jetFiles.size() > 1;
    }

    public static boolean isObjectLiteral(BindingContext bindingContext, ClassDescriptor declaration) {
        PsiElement psiElement = BindingContextUtils.descriptorToDeclaration(bindingContext, declaration);
        if (psiElement instanceof JetObjectDeclaration && ((JetObjectDeclaration) psiElement).isObjectLiteral()) {
            return true;
        }
        return false;
    }

    public static boolean isLocalFun(BindingContext bindingContext, DeclarationDescriptor fd) {
        PsiElement psiElement = BindingContextUtils.descriptorToDeclaration(bindingContext, fd);
        if (psiElement instanceof JetNamedFunction && psiElement.getParent() instanceof JetBlockExpression) {
            return true;
        }
        return false;
    }

    public static boolean isLocalNamedFun(BindingContext bindingContext, DeclarationDescriptor fd) {
        PsiElement psiElement = BindingContextUtils.descriptorToDeclaration(bindingContext, fd);
        if (psiElement instanceof JetNamedFunction) {
            final DeclarationDescriptor declaration = fd.getContainingDeclaration();
            return declaration instanceof FunctionDescriptor || declaration instanceof PropertyDescriptor;
        }
        return false;
    }
}
