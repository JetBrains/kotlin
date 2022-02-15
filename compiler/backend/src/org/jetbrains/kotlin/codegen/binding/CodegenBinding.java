/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.binding;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.JvmCodegenUtil;
import org.jetbrains.kotlin.backend.common.SamType;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.codegen.when.WhenByEnumsMapping;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilsKt;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils;
import org.jetbrains.kotlin.util.slicedMap.BasicWritableSlice;
import org.jetbrains.kotlin.util.slicedMap.Slices;
import org.jetbrains.kotlin.util.slicedMap.WritableSlice;
import org.jetbrains.kotlin.utils.KotlinExceptionWithAttachments;
import org.jetbrains.org.objectweb.asm.Type;

import java.util.*;

import static org.jetbrains.kotlin.resolve.BindingContext.*;

public class CodegenBinding {
    public static final WritableSlice<ClassDescriptor, MutableClosure> CLOSURE = Slices.createSimpleSlice();

    public static final WritableSlice<CallableDescriptor, ClassDescriptor> CLASS_FOR_CALLABLE = Slices.createSimpleSlice();

    public static final WritableSlice<ClassDescriptor, Type> ASM_TYPE = Slices.createSimpleSlice();

    public static final WritableSlice<ClassDescriptor, Boolean> ENUM_ENTRY_CLASS_NEED_SUBCLASS = Slices.createSimpleSetSlice();

    private static final WritableSlice<ClassDescriptor, Collection<ClassDescriptor>> INNER_CLASSES = Slices.createSimpleSlice();

    public static final WritableSlice<KtExpression, SamType> SAM_VALUE = Slices.createSimpleSlice();

    public static final WritableSlice<KtCallElement, KtExpression> SAM_CONSTRUCTOR_TO_ARGUMENT = Slices.createSimpleSlice();

    public static final WritableSlice<KtWhenExpression, WhenByEnumsMapping> MAPPING_FOR_WHEN_BY_ENUM = Slices.createSimpleSlice();

    public static final WritableSlice<String, List<WhenByEnumsMapping>> MAPPINGS_FOR_WHENS_BY_ENUM_IN_CLASS_FILE =
            Slices.createSimpleSlice();

    public static final WritableSlice<FunctionDescriptor, FunctionDescriptor> SUSPEND_FUNCTION_TO_JVM_VIEW =
            Slices.createSimpleSlice();

    public static final WritableSlice<FunctionDescriptor, Boolean> CAPTURES_CROSSINLINE_LAMBDA =
            Slices.createSimpleSlice();

    public static final WritableSlice<ValueParameterDescriptor, Boolean> CALL_SITE_IS_SUSPEND_FOR_CROSSINLINE_LAMBDA =
            Slices.createSimpleSlice();

    public static final WritableSlice<ClassDescriptor, Boolean> RECURSIVE_SUSPEND_CALLABLE_REFERENCE =
            Slices.createSimpleSlice();

    public static final WritableSlice<ValueParameterDescriptor, ValueParameterDescriptor> PARAMETER_SYNONYM =
            Slices.createSimpleSlice();

    public static final WritableSlice<Type, List<VariableDescriptorWithAccessors>> DELEGATED_PROPERTIES_WITH_METADATA =
            Slices.createSimpleSlice();
    public static final WritableSlice<VariableDescriptorWithAccessors, Type> DELEGATED_PROPERTY_METADATA_OWNER =
            Slices.createSimpleSlice();
    public static final WritableSlice<VariableDescriptor, VariableDescriptor> LOCAL_VARIABLE_PROPERTY_METADATA =
            Slices.createSimpleSlice();
    public static final WritableSlice<FunctionDescriptor, Boolean> PROPERTY_METADATA_REQUIRED_FOR_OPERATOR_CALL =
            Slices.createSimpleSlice();
    public static final WritableSlice<VariableDescriptorWithAccessors, Boolean> DELEGATED_PROPERTY_WITH_OPTIMIZED_METADATA =
            Slices.createSimpleSlice();

    public static final WritableSlice<FunctionDescriptor, String> CALL_LABEL_FOR_LAMBDA_ARGUMENT = Slices.createSimpleSlice();

    static {
        BasicWritableSlice.initSliceDebugNames(CodegenBinding.class);
    }

    private CodegenBinding() {
    }

    @Nullable
    public static List<LocalVariableDescriptor> getLocalDelegatedProperties(@NotNull BindingContext bindingContext, @NotNull Type owner) {
        List<VariableDescriptorWithAccessors> properties = bindingContext.get(DELEGATED_PROPERTIES_WITH_METADATA, owner);
        return properties == null ? null : CollectionsKt.filterIsInstance(properties, LocalVariableDescriptor.class);
    }

    public static void initTrace(@NotNull GenerationState state, Collection<KtFile> files) {
        CodegenAnnotatingVisitor visitor = new CodegenAnnotatingVisitor(state);
        for (KtFile file : allFilesInPackages(state.getBindingContext(), files)) {
            file.accept(visitor);
            if (file instanceof KtCodeFragment) {
                PsiElement context = file.getContext();
                if (context != null) {
                    PsiFile contextFile = context.getContainingFile();
                    if (contextFile != null) {
                        contextFile.accept(visitor);
                    }
                }
            }
        }
    }

    public static boolean enumEntryNeedSubclass(BindingContext bindingContext, KtEnumEntry enumEntry) {
        return enumEntryNeedSubclass(bindingContext, bindingContext.get(CLASS, enumEntry));
    }

    public static boolean enumEntryNeedSubclass(BindingContext bindingContext, ClassDescriptor classDescriptor) {
        return Boolean.TRUE.equals(bindingContext.get(ENUM_ENTRY_CLASS_NEED_SUBCLASS, classDescriptor));
    }

    @NotNull
    public static Type asmTypeForAnonymousClass(@NotNull BindingContext bindingContext, @NotNull KtElement expression) {
        Type result = asmTypeForAnonymousClassOrNull(bindingContext, expression);
        if (result == null) {
            throw new KotlinExceptionWithAttachments("Couldn't compute ASM type for expression")
                    .withAttachment("expression.kt", PsiUtilsKt.getElementTextWithContext(expression));
        }

        return result;
    }

    @Nullable
    public static Type asmTypeForAnonymousClassOrNull(@NotNull BindingContext bindingContext, @NotNull KtElement expression) {
        if (expression instanceof KtObjectLiteralExpression) {
            expression = ((KtObjectLiteralExpression) expression).getObjectDeclaration();
        }

        ClassDescriptor descriptor = bindingContext.get(CLASS, expression);
        if (descriptor != null) {
            return bindingContext.get(ASM_TYPE, descriptor);
        }

        SimpleFunctionDescriptor functionDescriptor = bindingContext.get(FUNCTION, expression);
        if (functionDescriptor != null) {
            return asmTypeForAnonymousClassOrNull(bindingContext, functionDescriptor);
        }

        VariableDescriptor variableDescriptor = bindingContext.get(VARIABLE, expression);
        if (variableDescriptor != null) {
            return asmTypeForAnonymousClassOrNull(bindingContext, variableDescriptor);
        }

        return null;
    }

    @NotNull
    public static Type asmTypeForAnonymousClass(@NotNull BindingContext bindingContext, @NotNull CallableDescriptor descriptor) {
        Type result = asmTypeForAnonymousClassOrNull(bindingContext, descriptor);
        if (result == null) {
            throw new IllegalStateException("Type must not be null: " + descriptor);
        }

        return result;
    }

    @Nullable
    public static Type asmTypeForAnonymousClassOrNull(@NotNull BindingContext bindingContext, @NotNull CallableDescriptor descriptor) {
        ClassDescriptor classForCallable = bindingContext.get(CLASS_FOR_CALLABLE, descriptor);
        if (classForCallable == null) {
            return null;
        }
        return bindingContext.get(ASM_TYPE, classForCallable);
    }

    public static boolean canHaveOuter(@NotNull BindingContext bindingContext, @NotNull ClassDescriptor classDescriptor) {
        if (classDescriptor.getKind() != ClassKind.CLASS) {
            return false;
        }

        MutableClosure closure = bindingContext.get(CLOSURE, classDescriptor);
        if (closure == null || closure.getEnclosingClass() == null) {
            return false;
        }

        DeclarationDescriptor containingDeclaration = classDescriptor.getContainingDeclaration();
        return classDescriptor.isInner()
               || containingDeclaration instanceof ScriptDescriptor
               || !(containingDeclaration instanceof ClassDescriptor);
    }

    @NotNull
    static MutableClosure recordClosure(
            @NotNull BindingTrace trace,
            @NotNull ClassDescriptor classDescriptor,
            @Nullable ClassDescriptor enclosing,
            @NotNull Type asmType
    ) {
        KtElement element = (KtElement) DescriptorToSourceUtils.descriptorToDeclaration(classDescriptor);
        assert element != null : "No source element for " + classDescriptor;

        MutableClosure closure = new MutableClosure(classDescriptor, enclosing);

        if (classDescriptor.isInner()) {
            closure.setNeedsCaptureOuterClass();
        }

        trace.record(ASM_TYPE, classDescriptor, asmType);
        trace.record(CLOSURE, classDescriptor, closure);

        // Note: at the moment this is needed for light classes only
        // TODO: refactor this out
        if (enclosing != null && !JvmCodegenUtil.isArgumentWhichWillBeInlined(trace.getBindingContext(), classDescriptor)) {
            recordInnerClass(trace, enclosing, classDescriptor);
        }

        return closure;
    }

    private static void recordInnerClass(
            @NotNull BindingTrace bindingTrace,
            @NotNull ClassDescriptor outer,
            @NotNull ClassDescriptor inner
    ) {
        Collection<ClassDescriptor> innerClasses = bindingTrace.get(INNER_CLASSES, outer);
        if (innerClasses == null) {
            innerClasses = new ArrayList<>(1);
            bindingTrace.record(INNER_CLASSES, outer, innerClasses);
        }
        innerClasses.add(inner);
    }

    @NotNull
    private static Collection<KtFile> allFilesInPackages(BindingContext bindingContext, Collection<KtFile> files) {
        // todo: we use Set and add given files but ignoring other scripts because something non-clear kept in binding
        // for scripts especially in case of REPL

        Set<FqName> names = new HashSet<>();
        for (KtFile file : files) {
            if (!file.isScript()) {
                names.add(file.getPackageFqName());
            }
        }

        Set<KtFile> answer = new HashSet<>();
        answer.addAll(files);

        for (FqName name : names) {
            Collection<KtFile> jetFiles = bindingContext.get(PACKAGE_TO_FILES, name);
            if (jetFiles != null) {
                answer.addAll(jetFiles);
            }
        }

        List<KtFile> sortedAnswer = new ArrayList<>(answer);

        sortedAnswer.sort(Comparator.comparing((KtFile file) -> {
            VirtualFile virtualFile = file.getVirtualFile();
            assert virtualFile != null : "VirtualFile is null for KtFile: " + file.getName();
            return virtualFile.getPath();
        }));

        return sortedAnswer;
    }

    @NotNull
    public static Type getAsmType(@NotNull BindingContext bindingContext, @NotNull ClassDescriptor klass) {
        Type type = bindingContext.get(ASM_TYPE, klass);
        if (type == null) {
            throw new IllegalStateException("Type is not yet recorded for " + klass);
        }
        return type;
    }

    @NotNull
    public static VariableDescriptor getDelegatedLocalVariableMetadata(
            @NotNull VariableDescriptor variableDescriptor,
            @NotNull BindingContext bindingContext
    ) {
        VariableDescriptor metadataVariableDescriptor = bindingContext.get(LOCAL_VARIABLE_PROPERTY_METADATA, variableDescriptor);
        assert metadataVariableDescriptor != null : "Metadata for local delegated property should be not null: " + variableDescriptor;
        return metadataVariableDescriptor;
    }
}
