/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen.binding;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.JvmCodegenUtil;
import org.jetbrains.kotlin.codegen.SamType;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.codegen.when.WhenByEnumsMapping;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.fileClasses.JvmFileClassesProvider;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilsKt;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils;
import org.jetbrains.kotlin.util.slicedMap.BasicWritableSlice;
import org.jetbrains.kotlin.util.slicedMap.Slices;
import org.jetbrains.kotlin.util.slicedMap.WritableSlice;
import org.jetbrains.org.objectweb.asm.Type;

import java.util.*;

import static org.jetbrains.kotlin.resolve.BindingContext.*;

public class CodegenBinding {
    public static final WritableSlice<ClassDescriptor, MutableClosure> CLOSURE = Slices.createSimpleSlice();

    public static final WritableSlice<CallableDescriptor, ClassDescriptor> CLASS_FOR_CALLABLE = Slices.createSimpleSlice();

    public static final WritableSlice<ClassDescriptor, Type> ASM_TYPE = Slices.createSimpleSlice();

    public static final WritableSlice<ClassDescriptor, Boolean> ENUM_ENTRY_CLASS_NEED_SUBCLASS = Slices.createSimpleSetSlice();

    public static final WritableSlice<ClassDescriptor, Collection<ClassDescriptor>> INNER_CLASSES = Slices.createSimpleSlice();

    public static final WritableSlice<KtExpression, SamType> SAM_VALUE = Slices.createSimpleSlice();

    public static final WritableSlice<KtCallElement, KtExpression> SAM_CONSTRUCTOR_TO_ARGUMENT = Slices.createSimpleSlice();

    public static final WritableSlice<KtWhenExpression, WhenByEnumsMapping> MAPPING_FOR_WHEN_BY_ENUM = Slices.createSimpleSlice();

    public static final WritableSlice<String, List<WhenByEnumsMapping>> MAPPINGS_FOR_WHENS_BY_ENUM_IN_CLASS_FILE =
            Slices.createSimpleSlice();

    public static final WritableSlice<VariableDescriptor, VariableDescriptor> LOCAL_VARIABLE_DELEGATE =
            Slices.createSimpleSlice();

    public static final WritableSlice<VariableDescriptor, VariableDescriptor> LOCAL_VARIABLE_PROPERTY_METADATA =
            Slices.createSimpleSlice();

    public static final WritableSlice<FunctionDescriptor, FunctionDescriptor> SUSPEND_FUNCTION_TO_JVM_VIEW =
            Slices.createSimpleSlice();

    public static final WritableSlice<ValueParameterDescriptor, ValueParameterDescriptor> PARAMETER_SYNONYM =
            Slices.createSimpleSlice();

    static {
        BasicWritableSlice.initSliceDebugNames(CodegenBinding.class);
    }

    private CodegenBinding() {
    }

    public static void initTrace(@NotNull GenerationState state) {
        CodegenAnnotatingVisitor visitor = new CodegenAnnotatingVisitor(state);
        for (KtFile file : allFilesInPackages(state.getBindingContext(), state.getFiles())) {
            file.accept(visitor);
        }
    }

    public static boolean enumEntryNeedSubclass(BindingContext bindingContext, KtEnumEntry enumEntry) {
        return enumEntryNeedSubclass(bindingContext, bindingContext.get(CLASS, enumEntry));
    }

    public static boolean enumEntryNeedSubclass(BindingContext bindingContext, ClassDescriptor classDescriptor) {
        return Boolean.TRUE.equals(bindingContext.get(ENUM_ENTRY_CLASS_NEED_SUBCLASS, classDescriptor));
    }

    @NotNull
    public static ClassDescriptor anonymousClassForCallable(
            @NotNull BindingContext bindingContext,
            @NotNull CallableDescriptor descriptor
    ) {
        //noinspection ConstantConditions
        return bindingContext.get(CLASS_FOR_CALLABLE, descriptor);
    }

    @NotNull
    public static Type asmTypeForAnonymousClass(@NotNull BindingContext bindingContext, @NotNull KtElement expression) {
        if (expression instanceof KtObjectLiteralExpression) {
            expression = ((KtObjectLiteralExpression) expression).getObjectDeclaration();
        }

        ClassDescriptor descriptor = bindingContext.get(CLASS, expression);
        if (descriptor != null) {
            return getAsmType(bindingContext, descriptor);
        }

        SimpleFunctionDescriptor functionDescriptor = bindingContext.get(FUNCTION, expression);
        if (functionDescriptor != null) {
            return asmTypeForAnonymousClass(bindingContext, functionDescriptor);
        }

        VariableDescriptor variableDescriptor = bindingContext.get(VARIABLE, expression);
        if (variableDescriptor != null) {
            return asmTypeForAnonymousClass(bindingContext, variableDescriptor);
        }

        throw new IllegalStateException("Couldn't compute ASM type for " + PsiUtilsKt.getElementTextWithContext(expression));
    }

    @NotNull
    public static Type asmTypeForAnonymousClass(@NotNull BindingContext bindingContext, @NotNull CallableDescriptor descriptor) {
        return getAsmType(bindingContext, anonymousClassForCallable(bindingContext, descriptor));
    }

    public static boolean canHaveOuter(@NotNull BindingContext bindingContext, @NotNull ClassDescriptor classDescriptor) {
        if (classDescriptor.getKind() != ClassKind.CLASS) {
            return false;
        }

        MutableClosure closure = bindingContext.get(CLOSURE, classDescriptor);
        if (closure == null || closure.getEnclosingClass() == null) {
            return false;
        }

        return classDescriptor.isInner() || !(classDescriptor.getContainingDeclaration() instanceof ClassDescriptor);
    }

    @NotNull
    static MutableClosure recordClosure(
            @NotNull BindingTrace trace,
            @NotNull ClassDescriptor classDescriptor,
            @Nullable ClassDescriptor enclosing,
            @NotNull Type asmType,
            @NotNull JvmFileClassesProvider fileClassesManager
    ) {
        KtElement element = (KtElement) DescriptorToSourceUtils.descriptorToDeclaration(classDescriptor);
        assert element != null : "No source element for " + classDescriptor;

        MutableClosure closure = new MutableClosure(classDescriptor, enclosing);

        if (classDescriptor.isInner()) {
            closure.setCaptureThis();
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
        assert type != null : "Type is not yet recorded for " + klass;
        return type;
    }

    @NotNull
    public static Collection<ClassDescriptor> getAllInnerClasses(
            @NotNull BindingContext bindingContext, @NotNull ClassDescriptor outermostClass
    ) {
        Collection<ClassDescriptor> innerClasses = bindingContext.get(INNER_CLASSES, outermostClass);
        if (innerClasses == null || innerClasses.isEmpty()) return Collections.emptySet();

        Set<ClassDescriptor> allInnerClasses = new HashSet<>();

        Deque<ClassDescriptor> stack = new ArrayDeque<>(innerClasses);
        do {
            ClassDescriptor currentClass = stack.pop();
            if (allInnerClasses.add(currentClass)) {
                Collection<ClassDescriptor> nextClasses = bindingContext.get(INNER_CLASSES, currentClass);
                if (nextClasses != null) {
                    for (ClassDescriptor nextClass : nextClasses) {
                        stack.push(nextClass);
                    }
                }
            }
        }
        while (!stack.isEmpty());

        return allInnerClasses;
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
