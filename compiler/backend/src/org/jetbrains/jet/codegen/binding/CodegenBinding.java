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
import org.jetbrains.asm4.Type;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.ClassDescriptorImpl;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaClassDescriptor;
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
import static org.jetbrains.jet.lang.resolve.DescriptorUtils.isEnumClass;

public class CodegenBinding {
    public static final WritableSlice<ClassDescriptor, MutableClosure> CLOSURE = Slices.createSimpleSlice();

    public static final WritableSlice<FunctionDescriptor, ClassDescriptor> CLASS_FOR_FUNCTION = Slices.createSimpleSlice();

    public static final WritableSlice<ScriptDescriptor, ClassDescriptor> CLASS_FOR_SCRIPT = Slices.createSimpleSlice();

    public static final WritableSlice<ClassDescriptor, Type> ASM_TYPE = Slices.createSimpleSlice();

    public static final WritableSlice<ClassDescriptor, Boolean> ENUM_ENTRY_CLASS_NEED_SUBCLASS = Slices.createSimpleSetSlice();

    public static final WritableSlice<ClassDescriptor, Collection<ClassDescriptor>> INNER_CLASSES = Slices.createSimpleSlice();

    public static final WritableSlice<JetExpression, JavaClassDescriptor> SAM_VALUE = Slices.createSimpleSlice();

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
    public static Type asmTypeForScriptDescriptor(BindingContext bindingContext, @NotNull ScriptDescriptor scriptDescriptor) {
        ClassDescriptor classDescriptor = bindingContext.get(CLASS_FOR_SCRIPT, scriptDescriptor);
        //noinspection ConstantConditions
        return asmType(bindingContext, classDescriptor);
    }

    @NotNull
    public static Type asmTypeForScriptPsi(BindingContext bindingContext, @NotNull JetScript script) {
        ScriptDescriptor scriptDescriptor = bindingContext.get(SCRIPT, script);
        if (scriptDescriptor == null) {
            throw new IllegalStateException("Script descriptor not found by PSI " + script);
        }
        return asmTypeForScriptDescriptor(bindingContext, scriptDescriptor);
    }

    public static ClassDescriptor enclosingClassDescriptor(BindingContext bindingContext, ClassDescriptor descriptor) {
        CalculatedClosure closure = bindingContext.get(CLOSURE, descriptor);
        return closure == null ? null : closure.getEnclosingClass();
    }

    @NotNull
    public static ClassDescriptor anonymousClassForFunction(
            @NotNull BindingContext bindingContext,
            @NotNull FunctionDescriptor descriptor
    ) {
        //noinspection ConstantConditions
        return bindingContext.get(CLASS_FOR_FUNCTION, descriptor);
    }

    @NotNull
    private static Type asmType(@NotNull BindingContext bindingContext, @NotNull ClassDescriptor descriptor) {
        //noinspection ConstantConditions
        return bindingContext.get(ASM_TYPE, descriptor);
    }

    @NotNull
    public static Type asmTypeForAnonymousClass(@NotNull BindingContext bindingContext, @NotNull JetElement expression) {
        if (expression instanceof JetObjectLiteralExpression) {
            JetObjectLiteralExpression jetObjectLiteralExpression = (JetObjectLiteralExpression) expression;
            expression = jetObjectLiteralExpression.getObjectDeclaration();
        }

        ClassDescriptor descriptor = bindingContext.get(CLASS, expression);
        if (descriptor == null) {
            SimpleFunctionDescriptor functionDescriptor = bindingContext.get(FUNCTION, expression);
            assert functionDescriptor != null;
            return asmTypeForAnonymousClass(bindingContext, functionDescriptor);
        }

        return asmType(bindingContext, descriptor);
    }

    @NotNull
    public static Type asmTypeForAnonymousClass(@NotNull BindingContext bindingContext, @NotNull FunctionDescriptor descriptor) {
        ClassDescriptor classDescriptor = anonymousClassForFunction(bindingContext, descriptor);
        return asmType(bindingContext, classDescriptor);
    }

    public static void registerClassNameForScript(
            BindingTrace bindingTrace,
            @NotNull ScriptDescriptor scriptDescriptor,
            @NotNull Type asmType
    ) {
        ClassDescriptorImpl classDescriptor =
                new ClassDescriptorImpl(scriptDescriptor, Name.special("<script-" + asmType.getInternalName() + ">"), Modality.FINAL,
                                        Collections.singleton(KotlinBuiltIns.getInstance().getAnyType()));
        classDescriptor.initialize(JetScope.EMPTY, Collections.<ConstructorDescriptor>emptySet(), null);

        recordClosure(bindingTrace, null, classDescriptor, null, asmType);

        bindingTrace.record(CLASS_FOR_SCRIPT, scriptDescriptor, classDescriptor);
    }

    public static boolean canHaveOuter(@NotNull BindingContext bindingContext, @NotNull ClassDescriptor classDescriptor) {
        if (classDescriptor.getKind() != ClassKind.CLASS) {
            return false;
        }

        ClassDescriptor enclosing = enclosingClassDescriptor(bindingContext, classDescriptor);
        if (enclosing == null) {
            return false;
        }

        return classDescriptor.isInner() || !(classDescriptor.getContainingDeclaration() instanceof ClassDescriptor);
    }

    static void recordClosure(
            BindingTrace bindingTrace,
            @Nullable JetElement element,
            ClassDescriptor classDescriptor,
            @Nullable ClassDescriptor enclosing,
            Type asmType
    ) {
        JetDelegatorToSuperCall superCall = findSuperCall(bindingTrace.getBindingContext(), element);

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

        MutableClosure closure = new MutableClosure(superCall, enclosing, enclosingReceiver);

        assert PsiCodegenPredictor.checkPredictedNameFromPsi(bindingTrace, classDescriptor, asmType);
        bindingTrace.record(ASM_TYPE, classDescriptor, asmType);
        bindingTrace.record(CLOSURE, classDescriptor, closure);

        if (classDescriptor.isInner()) {
            closure.setCaptureThis();
        }

        if (enclosing != null) {
            recordInnerClass(bindingTrace, enclosing, classDescriptor);
        }
    }

    private static void recordInnerClass(
            @NotNull BindingTrace bindingTrace,
            @NotNull ClassDescriptor outer,
            @NotNull ClassDescriptor inner
    ) {
        Collection<ClassDescriptor> innerClasses = bindingTrace.get(INNER_CLASSES, outer);
        if (innerClasses == null) {
            innerClasses = new ArrayList<ClassDescriptor>();
            bindingTrace.record(INNER_CLASSES, outer, innerClasses);
        }
        innerClasses.add(inner);
    }

    public static void registerClassNameForScript(
            BindingTrace bindingTrace,
            @NotNull JetScript jetScript,
            @NotNull Type asmType
    ) {
        ScriptDescriptor descriptor = bindingTrace.getBindingContext().get(SCRIPT, jetScript);
        if (descriptor == null) {
            throw new IllegalStateException("Descriptor is not found for PSI " + jetScript);
        }
        registerClassNameForScript(bindingTrace, descriptor, asmType);
    }

    @NotNull
    public static Collection<JetFile> allFilesInNamespaces(BindingContext bindingContext, Collection<JetFile> files) {
        // todo: we use Set and add given files but ignoring other scripts because something non-clear kept in binding
        // for scripts especially in case of REPL

        HashSet<FqName> names = new HashSet<FqName>();
        for (JetFile file : files) {
            if (!file.isScript()) {
                names.add(JetPsiUtil.getFQName(file));
            }
        }

        HashSet<JetFile> answer = new HashSet<JetFile>();
        answer.addAll(files);

        for (FqName name : names) {
            Collection<JetFile> jetFiles = bindingContext.get(PACKAGE_TO_FILES, name);
            if (jetFiles != null) {
                answer.addAll(jetFiles);
            }
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

    public static boolean isLocalNamedFun(DeclarationDescriptor fd) {
        if (fd instanceof FunctionDescriptor) {
            FunctionDescriptor descriptor = (FunctionDescriptor) fd;
            return descriptor.getVisibility() == Visibilities.LOCAL && !descriptor.getName().isSpecial();
        }
        return false;
    }

    @NotNull
    public static Type getAsmType(@NotNull BindingTrace bindingTrace, @NotNull ClassDescriptor klass) {
        klass = (ClassDescriptor) klass.getOriginal();
        Type alreadyComputedType = bindingTrace.getBindingContext().get(ASM_TYPE, klass);
        if (alreadyComputedType != null) {
            return alreadyComputedType;
        }

        Type asmType = Type.getObjectType(getAsmTypeImpl(bindingTrace, klass));

        assert PsiCodegenPredictor.checkPredictedNameFromPsi(bindingTrace, klass, asmType);
        bindingTrace.record(ASM_TYPE, klass, asmType);
        return asmType;
    }

    @NotNull
    private static String getAsmTypeImpl(@NotNull BindingTrace bindingTrace, @NotNull ClassDescriptor klass) {
        DeclarationDescriptor container = klass.getContainingDeclaration();

        if (container instanceof PackageFragmentDescriptor) {
            String shortName = klass.getName().getIdentifier();
            FqName fqName = ((PackageFragmentDescriptor) container).getFqName();
            return fqName.isRoot() ? shortName : fqName.asString().replace('.', '/') + '/' + shortName;
        }

        assert container instanceof ClassDescriptor : "Unexpected container: " + container + " for " + klass;

        String containerInternalName = getAsmType(bindingTrace, (ClassDescriptor) container).getInternalName();
        if (klass.getKind() == ClassKind.OBJECT || klass.getKind() == ClassKind.CLASS_OBJECT) {
            if (isEnumClass(container)) {
                return containerInternalName;
            }
            else if (klass.getKind() == ClassKind.OBJECT) {
                return containerInternalName + "$" + klass.getName();
            }
            else {
                return containerInternalName + JvmAbi.CLASS_OBJECT_SUFFIX;
            }
        }
        return containerInternalName + "$" + klass.getName().getIdentifier();
    }

    public static boolean isVarCapturedInClosure(BindingContext bindingContext, DeclarationDescriptor descriptor) {
        if (!(descriptor instanceof VariableDescriptor) || descriptor instanceof PropertyDescriptor) return false;
        VariableDescriptor variableDescriptor = (VariableDescriptor) descriptor;
        return bindingContext.get(CAPTURED_IN_CLOSURE, variableDescriptor) != null && variableDescriptor.isVar();
    }

    public static boolean hasThis0(BindingContext bindingContext, ClassDescriptor classDescriptor) {
        //noinspection SuspiciousMethodCalls
        CalculatedClosure closure = bindingContext.get(CLOSURE, classDescriptor);
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
