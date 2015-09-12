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

package org.jetbrains.kotlin.resolve;

import com.google.common.base.Predicate;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.scopes.JetScope;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.kotlin.resolve.validation.SymbolUsageValidator;

import java.util.Collection;

import static org.jetbrains.kotlin.diagnostics.Errors.*;

public class QualifiedExpressionResolver {
    @NotNull private final SymbolUsageValidator symbolUsageValidator;
    @NotNull private final NewQualifiedExpressionResolver newQualifiedExpressionResolver;

    public QualifiedExpressionResolver(@NotNull SymbolUsageValidator symbolUsageValidator, @NotNull NewQualifiedExpressionResolver newQualifiedExpressionResolver) {
        this.symbolUsageValidator = symbolUsageValidator;
        this.newQualifiedExpressionResolver = newQualifiedExpressionResolver;
    }

    private static final Predicate<DeclarationDescriptor> CLASSIFIERS_AND_PACKAGE_VIEWS = new Predicate<DeclarationDescriptor>() {
        @Override
        public boolean apply(@Nullable DeclarationDescriptor descriptor) {
            return descriptor instanceof ClassifierDescriptor || descriptor instanceof PackageViewDescriptor;
        }
    };

    public enum LookupMode {
        // Only classifier and packages are resolved
        ONLY_CLASSES_AND_PACKAGES,

        // Resolve all descriptors
        EVERYTHING
    }

    @NotNull
    public JetScope processImportReference(
            @NotNull JetImportDirective importDirective,
            @NotNull ModuleDescriptor moduleDescriptor,
            @NotNull BindingTrace trace,
            @NotNull LookupMode lookupMode
    ) {
        // todo fix shouldBeVisibleFrom
        return newQualifiedExpressionResolver.processImportReference(importDirective, moduleDescriptor, trace, moduleDescriptor);
    }
}
