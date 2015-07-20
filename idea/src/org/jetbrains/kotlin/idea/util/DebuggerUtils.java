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

package org.jetbrains.kotlin.idea.util;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.LibraryUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import kotlin.KotlinPackage;
import kotlin.Pair;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor;
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionFacade;
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils;
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde;
import org.jetbrains.kotlin.idea.debugger.DebuggerPackage;
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils;
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.CompositeBindingContext;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.inline.InlineUtil;
import org.jetbrains.kotlin.resolve.jvm.JvmClassName;
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode;
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedSimpleFunctionDescriptor;

import java.util.*;

import static org.jetbrains.kotlin.idea.stubindex.PackageIndexUtil.findFilesWithExactPackage;

public class DebuggerUtils {
    private DebuggerUtils() {
    }

    @Nullable
    public static JetFile findSourceFileForClass(
            @NotNull Project project,
            @NotNull GlobalSearchScope searchScope,
            @NotNull final JvmClassName className,
            @NotNull final String fileName,
            final int lineNumber
    ) {
        Collection<JetFile> filesInPackage = findFilesWithExactPackage(className.getPackageFqName(), searchScope, project);
        Collection<JetFile> filesWithExactName = Collections2.filter(filesInPackage, new Predicate<JetFile>() {
            @Override
            public boolean apply(@Nullable JetFile file) {
                return file != null && file.getName().equals(fileName);
            }
        });

        if (filesWithExactName.isEmpty()) return null;

        if (filesWithExactName.size() == 1) {
            return filesWithExactName.iterator().next();
        }

        if (isPackageClassName(className)) {
            for (JetFile file : filesWithExactName) {
                boolean hasTopLevelMembers = KotlinPackage.any(file.getDeclarations(), new Function1<JetDeclaration, Boolean>() {
                    @Override
                    public Boolean invoke(JetDeclaration declaration) {
                        return !(declaration instanceof JetClassOrObject);
                    }
                });
                if (hasTopLevelMembers) return file;
            }
        }

        if (isPackagePartClassName(className)) {
            JetFile file = getFileForPackagePartPrefixedName(filesWithExactName, className.getInternalName());
            if (file != null) {
                return file;
            }

            boolean isInLibrary = KotlinPackage.all(filesWithExactName, new Function1<JetFile, Boolean>() {
                @Override
                public Boolean invoke(JetFile file) {
                    return LibraryUtil.findLibraryEntry(file.getVirtualFile(), file.getProject()) != null;
                }
            });

            if (isInLibrary) {
                return KotlinPackage.singleOrNull(KotlinPackage.filter(filesWithExactName, new Function1<JetFile, Boolean>() {
                    @Override
                    public Boolean invoke(JetFile file) {
                        Integer startLineOffset = CodeInsightUtils.getStartLineOffset(file, lineNumber);
                        assert startLineOffset != null : "Cannot find start line offset for file " + file.getName() + ", line " + lineNumber;
                        JetDeclaration elementAt = PsiTreeUtil.getParentOfType(file.findElementAt(startLineOffset), JetDeclaration.class);
                        return elementAt != null &&
                               className.getInternalName().equals(DebuggerPackage.findPackagePartInternalNameForLibraryFile(elementAt));
                    }
                }));
            }

            return null;
        }

        return filesWithExactName.iterator().next();
    }

    private static boolean isPackagePartClassName(JvmClassName className) {
        String packageName = PackageClassUtils.getPackageClassInternalName(className.getPackageFqName());

        String internalName = className.getInternalName();
        return !internalName.equals(packageName) && internalName.startsWith(packageName);
    }

    private static boolean isPackageClassName(JvmClassName className) {
        String packageName = PackageClassUtils.getPackageClassInternalName(className.getPackageFqName());

        return packageName.equals(className.getInternalName());
    }

    @Nullable
    private static JetFile getFileForPackagePartPrefixedName(
            @NotNull Collection<JetFile> allPackageFiles,
            @NotNull String classInternalName
    ) {
        for (JetFile file : allPackageFiles) {
            String packagePartInternalName = PackagePartClassUtils.getPackagePartInternalName(file);
            if (classInternalName.startsWith(packagePartInternalName)) {
                return file;
            }
        }
        return null;
    }

    @NotNull
    public static Pair<BindingContext, List<JetFile>> analyzeInlinedFunctions(
            @NotNull ResolutionFacade resolutionFacadeForFile,
            @NotNull BindingContext bindingContextForFile,
            @NotNull JetFile file,
            boolean analyzeOnlyReifiedInlineFunctions
    ) {
        Set<JetElement> analyzedElements = new HashSet<JetElement>();
        BindingContext context = analyzeElementWithInline(
                resolutionFacadeForFile,
                bindingContextForFile,
                file,
                1,
                analyzedElements,
                !analyzeOnlyReifiedInlineFunctions);

        //We processing another files just to annotate anonymous classes within their inline functions
        //Bytecode not produced for them cause of filtering via generateClassFilter
        Set<JetFile> toProcess = new LinkedHashSet<JetFile>();
        toProcess.add(file);

        for (JetElement collectedElement : analyzedElements) {
            JetFile containingFile = collectedElement.getContainingJetFile();
            toProcess.add(containingFile);
        }

        return new Pair<BindingContext, List<JetFile>>(context, new ArrayList<JetFile>(toProcess));
    }

    @NotNull
    private static BindingContext analyzeElementWithInline(
            @NotNull ResolutionFacade resolutionFacade,
            @NotNull final BindingContext bindingContext,
            @NotNull JetElement element,
            int deep,
            @NotNull final Set<JetElement> analyzedElements,
            final boolean analyzeInlineFunctions
    ) {
        final Project project = element.getProject();
        final Set<JetNamedFunction> collectedElements = new HashSet<JetNamedFunction>();

        element.accept(new JetTreeVisitorVoid() {
            @Override
            public void visitExpression(@NotNull JetExpression expression) {
                super.visitExpression(expression);

                Call call = bindingContext.get(BindingContext.CALL, expression);
                if (call == null) return;

                ResolvedCall<?> resolvedCall = bindingContext.get(BindingContext.RESOLVED_CALL, call);
                checkResolveCall(resolvedCall);
            }

            @Override
            public void visitMultiDeclaration(@NotNull JetMultiDeclaration multiDeclaration) {
                super.visitMultiDeclaration(multiDeclaration);

                for (JetMultiDeclarationEntry entry : multiDeclaration.getEntries()) {
                    ResolvedCall<FunctionDescriptor> resolvedCall =
                            bindingContext.get(BindingContext.COMPONENT_RESOLVED_CALL, entry);
                    checkResolveCall(resolvedCall);
                }
            }

            @Override
            public void visitForExpression(@NotNull JetForExpression expression) {
                super.visitForExpression(expression);

                checkResolveCall(bindingContext.get(BindingContext.LOOP_RANGE_ITERATOR_RESOLVED_CALL, expression.getLoopRange()));
                checkResolveCall(bindingContext.get(BindingContext.LOOP_RANGE_HAS_NEXT_RESOLVED_CALL, expression.getLoopRange()));
                checkResolveCall(bindingContext.get(BindingContext.LOOP_RANGE_NEXT_RESOLVED_CALL, expression.getLoopRange()));
            }

            private void checkResolveCall(ResolvedCall<?> resolvedCall) {
                if (resolvedCall == null) return;

                CallableDescriptor descriptor = resolvedCall.getResultingDescriptor();
                if (descriptor instanceof DeserializedSimpleFunctionDescriptor) return;

                if (InlineUtil.isInline(descriptor) && (analyzeInlineFunctions || hasReifiedTypeParameters(descriptor))) {
                    PsiElement declaration = DescriptorToSourceUtilsIde.INSTANCE$.getAnyDeclaration(project, descriptor);
                    if (declaration != null && declaration instanceof JetNamedFunction && !analyzedElements.contains(declaration)) {
                        collectedElements.add((JetNamedFunction) declaration);
                    }
                }
            }
        });

        analyzedElements.add(element);

        if (!collectedElements.isEmpty() && deep < 10) {
            List<BindingContext> innerContexts = new ArrayList<BindingContext>();
            for (JetNamedFunction inlineFunctions : collectedElements) {
                JetExpression body = inlineFunctions.getBodyExpression();
                assert body != null : "Inline function should have a body: " + inlineFunctions.getText();

                BindingContext bindingContextForFunction = resolutionFacade.analyze(body, BodyResolveMode.FULL);
                innerContexts.add(analyzeElementWithInline(resolutionFacade, bindingContextForFunction, inlineFunctions, deep + 1,
                                                           analyzedElements, analyzeInlineFunctions));
            }

            innerContexts.add(bindingContext);

            analyzedElements.addAll(collectedElements);
            return CompositeBindingContext.Companion.create(innerContexts);
        }

        return bindingContext;
    }

    private static boolean hasReifiedTypeParameters(CallableDescriptor descriptor) {
        return Iterables.any(descriptor.getTypeParameters(), new Predicate<TypeParameterDescriptor>() {
            @Override
            public boolean apply(TypeParameterDescriptor input) {
                return input.isReified();
            }
        });
    }
}
