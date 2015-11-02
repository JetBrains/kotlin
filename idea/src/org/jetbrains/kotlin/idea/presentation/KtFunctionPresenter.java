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

package org.jetbrains.kotlin.idea.presentation;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.ItemPresentationProvider;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.*;

import java.util.Collection;

public class KtFunctionPresenter implements ItemPresentationProvider<KtFunction> {
    @Override
    public ItemPresentation getPresentation(@NotNull final KtFunction function) {
        if (function instanceof KtFunctionLiteral) return null;

        return new KotlinDefaultNamedDeclarationPresentation(function) {
            @Override
            public String getPresentableText() {
                StringBuilder presentation = new StringBuilder(function.getName() != null ? function.getName() : "");

                Collection<String> paramsStrings = Collections2.transform(function.getValueParameters(), new Function<KtParameter, String>() {
                    @Override
                    public String apply(KtParameter parameter) {
                        if (parameter != null) {
                            KtTypeReference reference = parameter.getTypeReference();
                            if (reference != null) {
                                String text = reference.getText();
                                if (text != null) {
                                    return text;
                                }
                            }
                        }

                        return "?";
                    }
                });

                presentation.append("(").append(StringUtils.join(paramsStrings, ",")).append(")");
                return presentation.toString();
            }

            @Override
            public String getLocationString() {
                if (function instanceof KtConstructor) {
                    FqName name = ((KtConstructor) function).getContainingClassOrObject().getFqName();
                    return name != null ? String.format("(in %s)", name) : "";
                }

                FqName name = function.getFqName();
                if (name != null) {
                    KtTypeReference receiverTypeRef = function.getReceiverTypeReference();
                    String extensionLocation = receiverTypeRef != null ? "for " + receiverTypeRef.getText() + " " : "";
                    return String.format("(%sin %s)", extensionLocation, name.parent());
                }

                return "";
            }
        };
    }
}
