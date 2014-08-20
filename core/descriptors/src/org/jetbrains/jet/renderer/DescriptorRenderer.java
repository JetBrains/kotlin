/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.renderer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeProjection;

import java.util.List;

public interface DescriptorRenderer extends Renderer<DeclarationDescriptor> {
    DescriptorRenderer COMPACT_WITH_MODIFIERS = new DescriptorRendererBuilder().setWithDefinedIn(false).build();

    DescriptorRenderer SOURCE_CODE = new DescriptorRendererBuilder()
            .setNormalizedVisibilities(true)
            .setWithDefinedIn(false)
            .setShortNames(false)
            .setShowInternalKeyword(false)
            .setOverrideRenderingPolicy(OverrideRenderingPolicy.RENDER_OVERRIDE)
            .setUnitReturnType(false).build();

    DescriptorRenderer SOURCE_CODE_SHORT_NAMES_IN_TYPES = new DescriptorRendererBuilder()
            .setNormalizedVisibilities(true)
            .setWithDefinedIn(false)
            .setShortNames(true)
            .setShowInternalKeyword(false)
            .setOverrideRenderingPolicy(OverrideRenderingPolicy.RENDER_OVERRIDE)
            .setUnitReturnType(false).build();

    DescriptorRenderer COMPACT = new DescriptorRendererBuilder()
            .setWithDefinedIn(false)
            .setModifiers().build();

    DescriptorRenderer COMPACT_WITH_SHORT_TYPES = new DescriptorRendererBuilder()
            .setWithDefinedIn(false)
            .setModifiers()
            .setShortNames(true)
            .setIncludeSynthesizedParameterNames(false).build();

    DescriptorRenderer STARTS_FROM_NAME = new DescriptorRendererBuilder()
            .setWithDefinedIn(false)
            .setModifiers()
            .setStartFromName(true).build();

    DescriptorRenderer ONLY_NAMES_WITH_SHORT_TYPES = new DescriptorRendererBuilder()
            .setWithDefinedIn(false)
            .setModifiers()
            .setShortNames(true)
            .setWithoutTypeParameters(true)
            .setWithoutFunctionParameterNames(true)
            .setReceiverAfterName(true)
            .setRenderClassObjectName(true)
            .setWithoutSuperTypes(true)
            .setStartFromName(true).build();

    DescriptorRenderer FQ_NAMES_IN_TYPES = new DescriptorRendererBuilder().build();

    DescriptorRenderer SHORT_NAMES_IN_TYPES = new DescriptorRendererBuilder().setShortNames(true).setIncludeSynthesizedParameterNames(false).build();

    DescriptorRenderer DEBUG_TEXT = new DescriptorRendererBuilder().setDebugMode(true).build();

    DescriptorRenderer HTML_COMPACT_WITH_MODIFIERS = new DescriptorRendererBuilder()
            .setWithDefinedIn(false)
            .setTextFormat(TextFormat.HTML).build();

    DescriptorRenderer HTML_NAMES_WITH_SHORT_TYPES = new DescriptorRendererBuilder()
            .setWithDefinedIn(false)
            .setShortNames(true)
            .setTextFormat(TextFormat.HTML).build();

    DescriptorRenderer HTML = new DescriptorRendererBuilder().setTextFormat(TextFormat.HTML).build();

    DescriptorRenderer HTML_FOR_UNINFERRED_TYPE_PARAMS = new DescriptorRendererBuilder()
            .setUninferredTypeParameterAsName(true)
            .setModifiers()
            .setShortNames(true)
            .setTextFormat(TextFormat.HTML).build();

    @NotNull
    String renderType(@NotNull JetType type);

    @NotNull
    String renderTypeArguments(@NotNull List<TypeProjection> typeArguments);

    @NotNull
    String renderAnnotation(@NotNull AnnotationDescriptor annotation);

    @NotNull
    @Override
    String render(@NotNull DeclarationDescriptor declarationDescriptor);

    @NotNull
    String renderFunctionParameters(@NotNull FunctionDescriptor functionDescriptor);

    enum TextFormat {
        PLAIN, HTML
    }

    enum OverrideRenderingPolicy {
        RENDER_OVERRIDE, RENDER_OPEN, RENDER_OPEN_OVERRIDE
    }

    enum Modifier {
        VISIBILITY, MODALITY, OVERRIDE, ANNOTATIONS, INNER, MEMBER_KIND
    }

    /** @see DefaultValueParameterHandler */
    interface ValueParametersHandler {
        void appendBeforeValueParameters(@NotNull FunctionDescriptor function, @NotNull StringBuilder stringBuilder);
        void appendAfterValueParameters(@NotNull FunctionDescriptor function, @NotNull StringBuilder stringBuilder);

        void appendBeforeValueParameter(@NotNull ValueParameterDescriptor parameter, @NotNull StringBuilder stringBuilder);
        void appendAfterValueParameter(@NotNull ValueParameterDescriptor parameter, @NotNull StringBuilder stringBuilder);
    }

    class DefaultValueParameterHandler implements ValueParametersHandler {
        @Override
        public void appendBeforeValueParameters(@NotNull FunctionDescriptor function, @NotNull StringBuilder stringBuilder) {
            stringBuilder.append("(");
        }

        @Override
        public void appendAfterValueParameters(@NotNull FunctionDescriptor function, @NotNull StringBuilder stringBuilder) {
            stringBuilder.append(")");
        }

        @Override
        public void appendBeforeValueParameter(@NotNull ValueParameterDescriptor parameter, @NotNull StringBuilder stringBuilder) {
        }

        @Override
        public void appendAfterValueParameter(@NotNull ValueParameterDescriptor parameter, @NotNull StringBuilder stringBuilder) {
            FunctionDescriptor function = (FunctionDescriptor) parameter.getContainingDeclaration();
            if (parameter.getIndex() != function.getValueParameters().size() - 1) {
                stringBuilder.append(", ");
            }
        }
    }
}
