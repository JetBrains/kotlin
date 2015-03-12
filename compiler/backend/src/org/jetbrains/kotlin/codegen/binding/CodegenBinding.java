/*
 * Copyright 2010-2015 JetBrains s.r.o.
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
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.codegen.JvmCodegenUtil;
import org.jetbrains.kotlin.codegen.SamType;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.codegen.when.WhenByEnumsMapping;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.scopes.JetScope;
import org.jetbrains.kotlin.util.slicedMap.BasicWritableSlice;
import org.jetbrains.kotlin.util.slicedMap.Slices;
import org.jetbrains.kotlin.util.slicedMap.WritableSlice;
import org.jetbrains.org.objectweb.asm.Type;

import java.util.*;

import static org.jetbrains.kotlin.codegen.JvmCodegenUtil.isInterface;
import static org.jetbrains.kotlin.resolve.BindingContext.*;
import static org.jetbrains.kotlin.resolve.DescriptorToSourceUtils.descriptorToDeclaration;
import static org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilPackage.getResolvedCall;
import static org.jetbrains.kotlin.resolve.source.SourcePackage.toSourceElement;

public class CodegenBinding {
    public static final WritableSlice<ClassDescriptor, MutableClosure> CLOSURE = Slices.createSimpleSlice();

    public static final WritableSlice<FunctionDescriptor, ClassDescriptor> CLASS_FOR_FUNCTION = Slices.createSimpleSlice();

    public static final WritableSlice<ScriptDescriptor, ClassDescriptor> CLASS_FOR_SCRIPT = Slices.createSimpleSlice();

    public static final WritableSlice<ClassDescriptor, Type> ASM_TYPE = Slices.createSimpleSlice();

    public static final WritableSlice<ClassDescriptor, Boolean> ENUM_ENTRY_CLASS_NEED_SUBCLASS = Slices.createSimpleSetSlice();

    public static final WritableSlice<ClassDescriptor, Collection<ClassDescriptor>> INNER_CLASSES = Slices.createSimpleSlice();

    public static final WritableSlice<JetExpression, SamType> SAM_VALUE = Slices.createSimpleSlice();

    public static final WritableSlice<JetCallExpression, JetExpression> SAM_CONSTRUCTOR_TO_ARGUMENT = Slices.createSimpleSlice();

    public static final WritableSlice<JetWhenExpression, WhenByEnumsMapping> MAPPING_FOR_WHEN_BY_ENUM =
            Slices.<JetWhenExpression, WhenByEnumsMapping>sliceBuilder().build();

    public static final WritableSlice<String, List<WhenByEnumsMapping>> MAPPINGS_FOR_WHENS_BY_ENUM_IN_CLASS_FILE =
            Slices.<String, List<WhenByEnumsMapping>>sliceBuilder().build();

    static {
        BasicWritableSlice.initSliceDebugNames(CodegenBinding.class);
    }

    private CodegenBinding() {
    }

    public static void initTrace(@NotNull GenerationState state) {
        CodegenAnnotatingVisitor visitor = new CodegenAnnotatingVisitor(state);
        for (JetFile file : allFilesInPackages(state.getBindingContext(), state.getFiles())) {
            file.accept(visitor);
        }
    }

    public static boolean enumEntryNeedSubclass(BindingContext bindingContext, JetEnumEntry enumEntry) {
        return enumEntryNeedSubclass(bindingContext, bindingContext.get(CLASS, enumEntry));
    }

    public static boolean enumEntryNeedSubclass(BindingContext bindingContext, ClassDescriptor classDescriptor) {
        return Boolean.TRUE.equals(bindingContext.get(ENUM_ENTRY_CLASS_NEED_SUBCLASS, classDescriptor));
    }

    // SCRIPT: Generate asmType for script, move to ScriptingUtil
    @NotNull
    public static Type asmTypeForScriptDescriptor(BindingContext bindingContext, @NotNull ScriptDescriptor scriptDescriptor) {
        ClassDescriptor classDescriptor = bindingContext.get(CLASS_FOR_SCRIPT, scriptDescriptor);
        //noinspection ConstantConditions
        return getAsmType(bindingContext, classDescriptor);
    }

    // SCRIPT: Generate asmType for script, move to ScriptingUtil
    @NotNull
    public static Type asmTypeForScriptPsi(BindingContext bindingContext, @NotNull JetScript script) {
        ScriptDescriptor scriptDescriptor = bindingContext.get(SCRIPT, script);
        if (scriptDescriptor == null) {
            throw new IllegalStateException("Script descriptor not found by PSI " + script);
        }
        return asmTypeForScriptDescriptor(bindingContext, scriptDescriptor);
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

        return getAsmType(bindingContext, descriptor);
    }

    @NotNull
    public static Type asmTypeForAnonymousClass(@NotNull BindingContext bindingContext, @NotNull FunctionDescriptor descriptor) {
        return getAsmType(bindingContext, anonymousClassForFunction(bindingContext, descriptor));
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

    static void recordClosure(
            @NotNull BindingTrace trace,
            @NotNull ClassDescriptor classDescriptor,
            @Nullable ClassDescriptor enclosing,
            @NotNull Type asmType
    ) {
        JetElement element = (JetElement) descriptorToDeclaration(classDescriptor);
        assert element != null : "No source element for " + classDescriptor;

        MutableClosure closure = new MutableClosure(classDescriptor, enclosing);

        if (classDescriptor.isInner()) {
            closure.setCaptureThis();
        }

        assert PsiCodegenPredictor.checkPredictedNameFromPsi(classDescriptor, asmType);
        trace.record(ASM_TYPE, classDescriptor, asmType);
        trace.record(CLOSURE, classDescriptor, closure);

        // Note: at the moment this is needed for light classes only
        // TODO: refactor this out
        if (enclosing != null && !JvmCodegenUtil.isLambdaWhichWillBeInlined(trace.getBindingContext(), classDescriptor)) {
            recordInnerClass(trace, enclosing, classDescriptor);
        }
    }

    private static void recordInnerClass(
            @NotNull BindingTrace bindingTrace,
            @NotNull ClassDescriptor outer,
            @NotNull ClassDescriptor inner
    ) {
        Collection<ClassDescriptor> innerClasses = bindingTrace.get(INNER_CLASSES, outer);
        if (innerClasses == null) {
            innerClasses = new ArrayList<ClassDescriptor>(1);
            bindingTrace.record(INNER_CLASSES, outer, innerClasses);
        }
        innerClasses.add(inner);
    }

    // SCRIPT: register asmType for script, move to ScriptingUtil
    public static void registerClassNameForScript(@NotNull BindingTrace trace, @NotNull JetScript script, @NotNull Type asmType) {
        ScriptDescriptor descriptor = trace.getBindingContext().get(SCRIPT, script);
        if (descriptor == null) {
            throw new IllegalStateException("Script descriptor is not found for PSI: " + JetPsiUtil.getElementTextWithContext(script));
        }

        String simpleName = asmType.getInternalName().substring(asmType.getInternalName().lastIndexOf('/') + 1);
        ClassDescriptorImpl classDescriptor =
                new ClassDescriptorImpl(descriptor, Name.special("<script-" + simpleName + ">"), Modality.FINAL,
                                        Collections.singleton(KotlinBuiltIns.getInstance().getAnyType()), toSourceElement(script));
        classDescriptor.initialize(JetScope.Empty.INSTANCE$, Collections.<ConstructorDescriptor>emptySet(), null);

        recordClosure(trace, classDescriptor, null, asmType);

        trace.record(CLASS_FOR_SCRIPT, descriptor, classDescriptor);
    }

    @NotNull
    private static Collection<JetFile> allFilesInPackages(BindingContext bindingContext, Collection<JetFile> files) {
        // todo: we use Set and add given files but ignoring other scripts because something non-clear kept in binding
        // for scripts especially in case of REPL

        // SCRIPT: collect fq names for files that are not scripts
        HashSet<FqName> names = new HashSet<FqName>();
        for (JetFile file : files) {
            if (!file.isScript()) {
                names.add(file.getPackageFqName());
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
            public int compare(@NotNull JetFile first, @NotNull JetFile second) {
                return path(first).compareTo(path(second));
            }
        });

        return sortedAnswer;
    }

    public static boolean isLocalNamedFun(@Nullable DeclarationDescriptor fd) {
        return isLocalFunOrLambda(fd) && !fd.getName().isSpecial();
    }

    /*named or not*/
    public static boolean isLocalFunOrLambda(@Nullable DeclarationDescriptor fd) {
        if (fd instanceof FunctionDescriptor) {
            FunctionDescriptor descriptor = (FunctionDescriptor) fd;
            return descriptor.getVisibility() == Visibilities.LOCAL;
        }
        return false;
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

        Set<ClassDescriptor> allInnerClasses = new HashSet<ClassDescriptor>();

        Deque<ClassDescriptor> stack = new ArrayDeque<ClassDescriptor>(innerClasses);
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
        } while (!stack.isEmpty());

        return allInnerClasses;
    }
}
