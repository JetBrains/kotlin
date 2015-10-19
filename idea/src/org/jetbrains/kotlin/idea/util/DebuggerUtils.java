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
import com.google.common.collect.Sets;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor;
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde;
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade;
import org.jetbrains.kotlin.idea.stubindex.StaticFacadeIndexUtil;
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

    private static final Set<String> KOTLIN_EXTENSIONS = Sets.newHashSet("kt", "kts");

    @Nullable
    public static KtFile findSourceFileForClass(
            @NotNull Project project,
            @NotNull GlobalSearchScope searchScope,
            @NotNull JvmClassName className,
            @NotNull final String fileName
    ) {
        String extension = FileUtilRt.getExtension(fileName);
        if (!KOTLIN_EXTENSIONS.contains(extension)) return null;
        if (DumbService.getInstance(project).isDumb()) return null;

        Collection<KtFile> filesInPackage = findFilesWithExactPackage(className.getPackageFqName(), searchScope, project);
        Collection<KtFile> filesWithExactName = Collections2.filter(filesInPackage, new Predicate<KtFile>() {
            @Override
            public boolean apply(@Nullable KtFile file) {
                return file != null && file.getName().equals(fileName);
            }
        });

        if (filesWithExactName.isEmpty()) return null;

        if (filesWithExactName.size() == 1) {
            return filesWithExactName.iterator().next();
        }

        // Static facade or inner class of such facade?
        FqName partFqName = className.getFqNameForClassNameWithoutDollars();
        Collection<KtFile> filesForPart = StaticFacadeIndexUtil.findFilesForFilePart(partFqName, searchScope, project);
        if (!filesForPart.isEmpty()) {
            for (KtFile file : filesForPart) {
                if (file.getName().equals(fileName)) {
                    return file;
                }
            }
            // Do not fall back to decompiled files (which have different name).
            return null;
        }

        return filesWithExactName.iterator().next();
    }

    @NotNull
    public static Pair<BindingContext, List<KtFile>> analyzeInlinedFunctions(
            @NotNull ResolutionFacade resolutionFacadeForFile,
            @NotNull BindingContext bindingContextForFile,
            @NotNull KtFile file,
            boolean analyzeOnlyReifiedInlineFunctions
    ) {
        Set<KtElement> analyzedElements = new HashSet<KtElement>();
        BindingContext context = analyzeElementWithInline(
                resolutionFacadeForFile,
                bindingContextForFile,
                file,
                1,
                analyzedElements,
                !analyzeOnlyReifiedInlineFunctions);

        //We processing another files just to annotate anonymous classes within their inline functions
        //Bytecode not produced for them cause of filtering via generateClassFilter
        Set<KtFile> toProcess = new LinkedHashSet<KtFile>();
        toProcess.add(file);

        for (KtElement collectedElement : analyzedElements) {
            KtFile containingFile = collectedElement.getContainingJetFile();
            toProcess.add(containingFile);
        }

        return new Pair<BindingContext, List<KtFile>>(context, new ArrayList<KtFile>(toProcess));
    }

    @NotNull
    public static Collection<KtElement> analyzeElementWithInline(
            @NotNull ResolutionFacade resolutionFacade,
            @NotNull BindingContext bindingContext,
            @NotNull KtNamedFunction function,
            boolean analyzeInlineFunctions
    ) {
        Set<KtElement> analyzedElements = new HashSet<KtElement>();
        analyzeElementWithInline(resolutionFacade, bindingContext, function, 1, analyzedElements, !analyzeInlineFunctions);
        return analyzedElements;
    }

    @NotNull
    private static BindingContext analyzeElementWithInline(
            @NotNull ResolutionFacade resolutionFacade,
            @NotNull final BindingContext bindingContext,
            @NotNull KtElement element,
            int deep,
            @NotNull final Set<KtElement> analyzedElements,
            final boolean analyzeInlineFunctions
    ) {
        final Project project = element.getProject();
        final Set<KtNamedFunction> collectedElements = new HashSet<KtNamedFunction>();

        element.accept(new KtTreeVisitorVoid() {
            @Override
            public void visitExpression(@NotNull KtExpression expression) {
                super.visitExpression(expression);

                Call call = bindingContext.get(BindingContext.CALL, expression);
                if (call == null) return;

                ResolvedCall<?> resolvedCall = bindingContext.get(BindingContext.RESOLVED_CALL, call);
                checkResolveCall(resolvedCall);
            }

            @Override
            public void visitMultiDeclaration(@NotNull KtMultiDeclaration multiDeclaration) {
                super.visitMultiDeclaration(multiDeclaration);

                for (KtMultiDeclarationEntry entry : multiDeclaration.getEntries()) {
                    ResolvedCall<FunctionDescriptor> resolvedCall =
                            bindingContext.get(BindingContext.COMPONENT_RESOLVED_CALL, entry);
                    checkResolveCall(resolvedCall);
                }
            }

            @Override
            public void visitForExpression(@NotNull KtForExpression expression) {
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
                    if (declaration != null && declaration instanceof KtNamedFunction && !analyzedElements.contains(declaration)) {
                        collectedElements.add((KtNamedFunction) declaration);
                    }
                }
            }
        });

        analyzedElements.add(element);

        if (!collectedElements.isEmpty() && deep < 10) {
            List<BindingContext> innerContexts = new ArrayList<BindingContext>();
            for (KtNamedFunction inlineFunctions : collectedElements) {
                KtExpression body = inlineFunctions.getBodyExpression();
                if (body != null) {
                    BindingContext bindingContextForFunction = resolutionFacade.analyze(body, BodyResolveMode.FULL);
                    innerContexts.add(analyzeElementWithInline(resolutionFacade, bindingContextForFunction, inlineFunctions, deep + 1,
                                                               analyzedElements, analyzeInlineFunctions));
                }
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
