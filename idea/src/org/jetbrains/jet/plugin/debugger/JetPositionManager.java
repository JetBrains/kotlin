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

package org.jetbrains.jet.plugin.debugger;

import com.intellij.debugger.NoDataException;
import com.intellij.debugger.PositionManager;
import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.engine.DebugProcess;
import com.intellij.debugger.requests.ClassPrepareRequestor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.*;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.request.ClassPrepareRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.codegen.ClassBuilderFactories;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.codegen.state.JetTypeMapper;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.calls.callUtil.CallUtilPackage;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedValueArgument;
import org.jetbrains.jet.lang.resolve.extension.InlineAnalyzerExtension;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.kotlin.PackagePartClassUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.types.lang.InlineStrategy;
import org.jetbrains.jet.lang.types.lang.InlineUtil;
import org.jetbrains.jet.plugin.caches.resolve.ResolvePackage;
import org.jetbrains.jet.plugin.util.DebuggerUtils;
import org.jetbrains.org.objectweb.asm.Type;

import java.util.*;

import static org.jetbrains.jet.codegen.binding.CodegenBinding.asmTypeForAnonymousClass;
import static org.jetbrains.jet.plugin.stubindex.PackageIndexUtil.findFilesWithExactPackage;

public class JetPositionManager implements PositionManager {
    private final DebugProcess myDebugProcess;
    private final WeakHashMap<FqName, CachedValue<JetTypeMapper>> myTypeMappers = new WeakHashMap<FqName, CachedValue<JetTypeMapper>>();

    public JetPositionManager(DebugProcess debugProcess) {
        myDebugProcess = debugProcess;
    }

    @Override
    @Nullable
    public SourcePosition getSourcePosition(@Nullable Location location) throws NoDataException {
        if (location == null) {
            throw new NoDataException();
        }
        PsiFile psiFile = getPsiFileByLocation(location);
        if (psiFile == null) {
            throw new NoDataException();
        }

        int lineNumber;
        try {
            lineNumber = location.lineNumber() - 1;
        }
        catch (InternalError e) {
            lineNumber = -1;
        }

        if (lineNumber >= 0) {
            return SourcePosition.createFromLine(psiFile, lineNumber);
        }

        throw new NoDataException();
    }

    @Nullable
    private PsiFile getPsiFileByLocation(@NotNull Location location) {
        String sourceName;
        try {
            sourceName = location.sourceName();
        }
        catch (AbsentInformationException e) {
            return null;
        }

        // JDI names are of form "package.Class$InnerClass"
        String referenceFqName = location.declaringType().name();
        String referenceInternalName = referenceFqName.replace('.', '/');
        JvmClassName className = JvmClassName.byInternalName(referenceInternalName);

        Project project = myDebugProcess.getProject();
        return DebuggerUtils.findSourceFileForClass(project, GlobalSearchScope.allScope(project), className, sourceName);
    }

    @NotNull
    @Override
    public List<ReferenceType> getAllClasses(SourcePosition sourcePosition) throws NoDataException {
        if (!(sourcePosition.getFile() instanceof JetFile)) {
            throw new NoDataException();
        }
        String name = classNameForPosition(sourcePosition);
        List<ReferenceType> result = new ArrayList<ReferenceType>();
        if (name != null) {
            result.addAll(myDebugProcess.getVirtualMachineProxy().classesByName(name));
        }
        return result;
    }

    @Nullable
    private String classNameForPosition(final SourcePosition sourcePosition) {
        final Ref<String> result = Ref.create();

        ApplicationManager.getApplication().runReadAction(new Runnable() {
            @Override
            @SuppressWarnings("unchecked")
            public void run() {
                JetFile file = (JetFile) sourcePosition.getFile();
                JetTypeMapper typeMapper = prepareTypeMapper(file);

                result.set(getClassNameForElement(sourcePosition.getElementAt(), typeMapper, file));
            }

        });

        return result.get();
    }

    @SuppressWarnings("unchecked")
    private static String getClassNameForElement(@Nullable PsiElement notPositionedElement, @NotNull JetTypeMapper typeMapper, @NotNull JetFile file) {
        PsiElement element = PsiTreeUtil.getParentOfType(notPositionedElement, JetClassOrObject.class, JetFunctionLiteral.class, JetNamedFunction.class);

        if (element instanceof JetClassOrObject) {
            return getJvmInternalNameForImpl(typeMapper, (JetClassOrObject) element);
        }
        else if (element instanceof JetFunctionLiteral) {
            if (isInlinedLambda((JetFunctionLiteral) element, typeMapper.getBindingContext())) {
                return getClassNameForElement(element.getParent(), typeMapper, file);
            } else {
                Type asmType = asmTypeForAnonymousClass(typeMapper.getBindingContext(), ((JetFunctionLiteral) element));
                return asmType.getInternalName();
            }
        }
        else if (element instanceof JetNamedFunction) {
            PsiElement parent = PsiTreeUtil.getParentOfType(element, JetClassOrObject.class, JetFunctionLiteralExpression.class, JetNamedFunction.class);
            if (parent instanceof JetClassOrObject) {
                return getJvmInternalNameForImpl(typeMapper, (JetClassOrObject) parent);
            }
            else if (parent instanceof JetFunctionLiteralExpression || parent instanceof JetNamedFunction) {
                Type asmType = asmTypeForAnonymousClass(typeMapper.getBindingContext(), (JetElement) element);
                return asmType.getInternalName();
            }
        }

        return PackagePartClassUtils.getPackagePartInternalName(file);
    }


    @Nullable
    private static String getJvmInternalNameForImpl(JetTypeMapper typeMapper, JetClassOrObject jetClass) {
        ClassDescriptor classDescriptor = typeMapper.getBindingContext().get(BindingContext.CLASS, jetClass);
        if (classDescriptor == null) {
            return null;
        }

        if (jetClass instanceof JetClass && ((JetClass) jetClass).isTrait()) {
            return typeMapper.mapTraitImpl(classDescriptor).getInternalName();
        }

        return typeMapper.mapClass(classDescriptor).getInternalName();
    }

    private JetTypeMapper prepareTypeMapper(final JetFile file) {
        final FqName fqName = file.getPackageFqName();
        CachedValue<JetTypeMapper> value = myTypeMappers.get(fqName);
        if(value == null) {
            value = CachedValuesManager.getManager(file.getProject()).createCachedValue(new CachedValueProvider<JetTypeMapper>() {
                @Override
                public Result<JetTypeMapper> compute() {
                    Project project = file.getProject();
                    Collection<JetFile> packageFiles = findFilesWithExactPackage(fqName, GlobalSearchScope.allScope(project), project);

                    AnalyzeExhaust analyzeExhaust = ResolvePackage.getAnalysisResultsForElements(packageFiles);
                    analyzeExhaust.throwIfError();

                    GenerationState state = new GenerationState(project, ClassBuilderFactories.THROW_EXCEPTION,
                                                                analyzeExhaust.getModuleDescriptor(), analyzeExhaust.getBindingContext(),
                                                                new ArrayList<JetFile>(packageFiles)
                    );
                    state.beforeCompile();
                    return new Result<JetTypeMapper>(state.getTypeMapper(), PsiModificationTracker.MODIFICATION_COUNT);
                }
            }, false);
            myTypeMappers.put(fqName, value);
        }

        return value.getValue();
    }

    @NotNull
    @Override
    public List<Location> locationsOfLine(ReferenceType type, SourcePosition position) throws NoDataException {
        if (!(position.getFile() instanceof JetFile)) {
            throw new NoDataException();
        }
        try {
            int line = position.getLine() + 1;
            List<Location> locations = myDebugProcess.getVirtualMachineProxy().versionHigher("1.4")
                                       ? type.locationsOfLine(DebugProcess.JAVA_STRATUM, null, line)
                                       : type.locationsOfLine(line);
            if (locations == null || locations.isEmpty()) throw new NoDataException();
            return locations;
        }
        catch (AbsentInformationException e) {
            throw new NoDataException();
        }
    }

    @Override
    public ClassPrepareRequest createPrepareRequest(ClassPrepareRequestor classPrepareRequestor,
                                                    SourcePosition sourcePosition) throws NoDataException {
        if (!(sourcePosition.getFile() instanceof JetFile)) {
            throw new NoDataException();
        }
        String className = classNameForPosition(sourcePosition);
        if (className == null) {
            return null;
        }
        return myDebugProcess.getRequestsManager().createClassPrepareRequest(classPrepareRequestor, className.replace('/', '.'));
    }

    @TestOnly
    public void addTypeMapper(JetFile file, final JetTypeMapper typeMapper) {
        FqName fqName = file.getPackageFqName();
        CachedValue<JetTypeMapper> value = CachedValuesManager.getManager(file.getProject()).createCachedValue(new CachedValueProvider<JetTypeMapper>() {
            @Override
            public Result<JetTypeMapper> compute() {
                return new Result<JetTypeMapper>(typeMapper, PsiModificationTracker.MODIFICATION_COUNT);
            }
        }, false);
        myTypeMappers.put(fqName, value);
    }


    public static boolean isInlinedLambda(@NotNull JetFunctionLiteral functionLiteral, @NotNull BindingContext context) {
        PsiElement functionLiteralExpression = functionLiteral.getParent();
        if (functionLiteralExpression == null) return false;

        PsiElement parent = functionLiteralExpression.getParent();

        PsiElement valueArgument = functionLiteralExpression;
        while (parent instanceof JetParenthesizedExpression ||
               parent instanceof JetBinaryExpressionWithTypeRHS ||
               parent instanceof JetLabeledExpression) {
            valueArgument = parent;
            parent = parent.getParent();
        }

        while (parent instanceof ValueArgument ||
               parent instanceof JetValueArgumentList) {
            parent = parent.getParent();
        }

        if (!(parent instanceof JetElement)) return false;

        ResolvedCall<?> call = CallUtilPackage.getResolvedCall((JetElement) parent, context);
        if (call == null) return false;

        InlineStrategy inlineType = InlineUtil.getInlineType(call.getResultingDescriptor());
        if (!inlineType.isInline()) return false;

        for (Map.Entry<ValueParameterDescriptor, ResolvedValueArgument> entry : call.getValueArguments().entrySet()) {
            ValueParameterDescriptor valueParameterDescriptor = entry.getKey();
            ResolvedValueArgument resolvedValueArgument = entry.getValue();

            for (ValueArgument next : resolvedValueArgument.getArguments()) {
                JetExpression expression = next.getArgumentExpression();
                if (valueArgument == expression) {
                    return InlineAnalyzerExtension.checkInlinableParameter(
                            valueParameterDescriptor, expression,
                            call.getResultingDescriptor(), null
                    );
                }
            }
        }
        return false;
    }

}
