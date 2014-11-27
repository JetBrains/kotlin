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
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.LibraryUtil;
import com.intellij.openapi.util.Pair;
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
import org.jetbrains.jet.analyzer.AnalysisResult;
import org.jetbrains.jet.codegen.AsmUtil;
import org.jetbrains.jet.codegen.ClassBuilderFactories;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.codegen.state.JetTypeMapper;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
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
import org.jetbrains.jet.plugin.caches.resolve.IdeaModuleInfo;
import org.jetbrains.jet.plugin.caches.resolve.KotlinCacheService;
import org.jetbrains.jet.plugin.caches.resolve.ResolvePackage;
import org.jetbrains.jet.plugin.codeInsight.CodeInsightUtils;
import org.jetbrains.jet.plugin.util.DebuggerUtils;
import org.jetbrains.org.objectweb.asm.Type;

import java.util.*;

import static org.jetbrains.jet.codegen.binding.CodegenBinding.asmTypeForAnonymousClass;
import static org.jetbrains.jet.plugin.stubindex.PackageIndexUtil.findFilesWithExactPackage;

public class JetPositionManager implements PositionManager {
    private final DebugProcess myDebugProcess;
    private final WeakHashMap<Pair<FqName, IdeaModuleInfo>, CachedValue<JetTypeMapper>> myTypeMappers = new WeakHashMap<Pair<FqName, IdeaModuleInfo>, CachedValue<JetTypeMapper>>();

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
            JetFunctionLiteral lambdaIfInside = getLambdaIfInside(location, (JetFile) psiFile, lineNumber);
            if (lambdaIfInside != null) {
                return SourcePosition.createFromElement(lambdaIfInside.getBodyExpression().getStatements().get(0));
            }
            return SourcePosition.createFromLine(psiFile, lineNumber);
        }

        throw new NoDataException();
    }

    private JetFunctionLiteral getLambdaIfInside(@NotNull Location location, @NotNull JetFile file, int lineNumber) {
        String currentLocationFqName = location.declaringType().name();
        if (currentLocationFqName == null) return null;

        Integer start = CodeInsightUtils.getStartLineOffset(file, lineNumber);
        Integer end = CodeInsightUtils.getEndLineOffset(file, lineNumber);
        if (start == null || end == null) return null;

        PsiElement[] literals = CodeInsightUtils.findElementsOfClassInRange(file, start, end, JetFunctionLiteral.class);
        if (literals == null || literals.length == 0) return null;

        boolean isInLibrary = LibraryUtil.findLibraryEntry(file.getVirtualFile(), file.getProject()) != null;
        JetTypeMapper typeMapper = !isInLibrary
                                    ? prepareTypeMapper(file)
                                    : createTypeMapperForLibraryFile(file.findElementAt(start), file);

        String currentLocationClassName = JvmClassName.byFqNameWithoutInnerClasses(new FqName(currentLocationFqName)).getInternalName();
        for (PsiElement literal : literals) {
            JetFunctionLiteral functionLiteral = (JetFunctionLiteral) literal;
            if (isInlinedLambda(functionLiteral, typeMapper.getBindingContext())) {
                continue;
            }

            String internalClassName = getClassNameForElement(literal.getFirstChild(), typeMapper, file, isInLibrary);
            if (internalClassName.equals(currentLocationClassName)) {
                return functionLiteral;
            }
        }

        return null;
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

        if (DumbService.getInstance(project).isDumb()) return null;

        return DebuggerUtils.findSourceFileForClass(project, GlobalSearchScope.allScope(project), className, sourceName, location.lineNumber() - 1);
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
                boolean isInLibrary = LibraryUtil.findLibraryEntry(file.getVirtualFile(), file.getProject()) != null;
                JetTypeMapper typeMapper = !isInLibrary ? prepareTypeMapper(file) : createTypeMapperForLibraryFile(sourcePosition.getElementAt(), file);
                result.set(getClassNameForElement(sourcePosition.getElementAt(), typeMapper, file, isInLibrary));
            }

        });

        return result.get();
    }

    @SuppressWarnings("unchecked")
    public static String getClassNameForElement(
            @Nullable PsiElement notPositionedElement,
            @NotNull JetTypeMapper typeMapper,
            @NotNull JetFile file,
            boolean isInLibrary
    ) {
        PsiElement element = getElementToCalculateClassName(notPositionedElement);

        if (element instanceof JetClassOrObject) {
            return getJvmInternalNameForImpl(typeMapper, (JetClassOrObject) element);
        }
        else if (element instanceof JetFunctionLiteral) {
            if (isInlinedLambda((JetFunctionLiteral) element, typeMapper.getBindingContext())) {
                return getClassNameForElement(element.getParent(), typeMapper, file, isInLibrary);
            } else {
                Type asmType = asmTypeForAnonymousClass(typeMapper.getBindingContext(), ((JetFunctionLiteral) element));
                return asmType.getInternalName();
            }
        }
        else if (element instanceof JetClassInitializer) {
            PsiElement parent = getElementToCalculateClassName(element.getParent());
            // Class-object initializer
            if (parent instanceof JetObjectDeclaration && ((JetObjectDeclaration) parent).isClassObject()) {
                return getClassNameForElement(parent.getParent(), typeMapper, file, isInLibrary);
            }
            return getClassNameForElement(element, typeMapper, file, isInLibrary);
        }
        else if (element instanceof JetProperty && (!((JetProperty) element).isTopLevel() || !isInLibrary)) {
            if (isInPropertyAccessor(notPositionedElement)) {
                JetClassOrObject classOrObject = PsiTreeUtil.getParentOfType(element, JetClassOrObject.class);
                if (classOrObject != null) {
                    return getJvmInternalNameForImpl(typeMapper, classOrObject);
                }
            }

            VariableDescriptor descriptor = (VariableDescriptor) typeMapper.getBindingContext().get(BindingContext.DECLARATION_TO_DESCRIPTOR, element);
            if (!(descriptor instanceof PropertyDescriptor)) {
                return getClassNameForElement(element.getParent(), typeMapper, file, isInLibrary);
            }

            return getJvmInternalNameForPropertyOwner(typeMapper, (PropertyDescriptor) descriptor);
        }
        else if (element instanceof JetNamedFunction) {
            PsiElement parent = getElementToCalculateClassName(element);
            if (parent instanceof JetClassOrObject) {
                return getJvmInternalNameForImpl(typeMapper, (JetClassOrObject) parent);
            }
            else if (parent != null) {
                Type asmType = asmTypeForAnonymousClass(typeMapper.getBindingContext(), (JetElement) element);
                return asmType.getInternalName();
            }
        }

        if (isInLibrary) {
            JetElement elementAtForLibraryFile = getElementToCreateTypeMapperForLibraryFile(notPositionedElement);
            assert elementAtForLibraryFile != null : "Couldn't find element at breakpoint for library file " + file.getName()
                   + (notPositionedElement == null ? "" : ", notPositionedElement = " + JetPsiUtil.getElementTextWithContext( (JetElement) notPositionedElement));
            return DebuggerPackage.findPackagePartInternalNameForLibraryFile(elementAtForLibraryFile);
        }

        return PackagePartClassUtils.getPackagePartInternalName(file);
    }

    @Nullable
    private static JetDeclaration getElementToCalculateClassName(@Nullable PsiElement notPositionedElement) {
        //noinspection unchecked
        return PsiTreeUtil.getParentOfType(notPositionedElement,
                                           JetClassOrObject.class,
                                           JetFunctionLiteral.class, JetNamedFunction.class,
                                           JetProperty.class,
                                           JetClassInitializer.class);
    }

    @NotNull
    public static String getJvmInternalNameForPropertyOwner(@NotNull JetTypeMapper typeMapper, @NotNull PropertyDescriptor descriptor) {
        return typeMapper.mapOwner(
                AsmUtil.isPropertyWithBackingFieldInOuterClass(descriptor) ? descriptor.getContainingDeclaration() : descriptor,
                true)
                .getInternalName();
    }

    private static boolean isInPropertyAccessor(@Nullable PsiElement element) {
        //noinspection unchecked
        return element instanceof  JetPropertyAccessor || ((JetElement) PsiTreeUtil.getParentOfType(element, JetProperty.class, JetPropertyAccessor.class)) instanceof JetPropertyAccessor;
    }

    @Nullable
    private static JetElement getElementToCreateTypeMapperForLibraryFile(@Nullable PsiElement element) {
        return element instanceof JetElement ? (JetElement) element : PsiTreeUtil.getParentOfType(element, JetElement.class);
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

    private static JetTypeMapper createTypeMapperForLibraryFile(@Nullable PsiElement notPositionedElement, @NotNull JetFile file) {
        JetElement element = getElementToCreateTypeMapperForLibraryFile(notPositionedElement);
        AnalysisResult analysisResult = ResolvePackage.analyzeAndGetResult(element);

        GenerationState state = new GenerationState(file.getProject(), ClassBuilderFactories.THROW_EXCEPTION,
                                                    analysisResult.getModuleDescriptor(),
                                                    analysisResult.getBindingContext(),
                                                    Collections.singletonList(file)
        );
        state.beforeCompile();
        return state.getTypeMapper();
    }

    private JetTypeMapper prepareTypeMapper(final JetFile file) {
        final Pair<FqName, IdeaModuleInfo> key = createKeyForTypeMapper(file);

        CachedValue<JetTypeMapper> value = myTypeMappers.get(key);
        if(value == null) {
            value = CachedValuesManager.getManager(file.getProject()).createCachedValue(new CachedValueProvider<JetTypeMapper>() {
                @Override
                public Result<JetTypeMapper> compute() {
                    Project project = file.getProject();
                    GlobalSearchScope packageFacadeScope = key.second.contentScope();
                    Collection<JetFile> packageFiles = findFilesWithExactPackage(key.first, packageFacadeScope, project);

                    AnalysisResult analysisResult = KotlinCacheService.OBJECT$.getInstance(project).getAnalysisResults(packageFiles);
                    analysisResult.throwIfError();

                    GenerationState state = new GenerationState(project, ClassBuilderFactories.THROW_EXCEPTION,
                                                                analysisResult.getModuleDescriptor(), analysisResult.getBindingContext(),
                                                                new ArrayList<JetFile>(packageFiles)
                    );
                    state.beforeCompile();
                    return new Result<JetTypeMapper>(state.getTypeMapper(), PsiModificationTracker.MODIFICATION_COUNT);
                }
            }, false);

            myTypeMappers.put(key, value);
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
        CachedValue<JetTypeMapper> value = CachedValuesManager.getManager(file.getProject()).createCachedValue(new CachedValueProvider<JetTypeMapper>() {
            @Override
            public Result<JetTypeMapper> compute() {
                return new Result<JetTypeMapper>(typeMapper, PsiModificationTracker.MODIFICATION_COUNT);
            }
        }, false);

        Pair<FqName, IdeaModuleInfo> key = createKeyForTypeMapper(file);
        myTypeMappers.put(key, value);
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

    private static Pair<FqName, IdeaModuleInfo> createKeyForTypeMapper(@NotNull JetFile file) {
        return new Pair<FqName, IdeaModuleInfo>(file.getPackageFqName(), ResolvePackage.getModuleInfo(file));
    }

}
