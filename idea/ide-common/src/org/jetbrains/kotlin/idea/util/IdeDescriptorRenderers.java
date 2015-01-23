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

import kotlin.Function1;
import org.jetbrains.kotlin.renderer.DescriptorRenderer;
import org.jetbrains.kotlin.renderer.DescriptorRendererBuilder;
import org.jetbrains.kotlin.renderer.NameShortness;
import org.jetbrains.kotlin.types.JetType;

public class IdeDescriptorRenderers {

    public static final Function1<JetType, JetType> APPROXIMATE_FLEXIBLE_TYPES = new Function1<JetType, JetType>() {
        @Override
        public JetType invoke(JetType type) {
            return UtilPackage.approximateFlexibleTypes(type, true);
        }
    };

    public static final Function1<JetType, JetType> APPROXIMATE_FLEXIBLE_TYPES_IN_ARGUMENTS = new Function1<JetType, JetType>() {
        @Override
        public JetType invoke(JetType type) {
            return UtilPackage.approximateFlexibleTypes(type, false);
        }
    };

    public static final DescriptorRenderer SOURCE_CODE = commonBuilder()
            .setNameShortness(NameShortness.SOURCE_CODE_QUALIFIED)
            .setTypeNormalizer(APPROXIMATE_FLEXIBLE_TYPES)
            .build();

    public static final DescriptorRenderer SOURCE_CODE_FOR_TYPE_ARGUMENTS = commonBuilder()
            .setNameShortness(NameShortness.SOURCE_CODE_QUALIFIED)
            .setTypeNormalizer(APPROXIMATE_FLEXIBLE_TYPES_IN_ARGUMENTS)
            .build();

    public static final DescriptorRenderer SOURCE_CODE_SHORT_NAMES_IN_TYPES = commonBuilder()
            .setNameShortness(NameShortness.SHORT)
            .setTypeNormalizer(APPROXIMATE_FLEXIBLE_TYPES)
            .build();

    private static DescriptorRendererBuilder commonBuilder() {
        return new DescriptorRendererBuilder()
                .setNormalizedVisibilities(true)
                .setWithDefinedIn(false)
                .setShowInternalKeyword(false)
                .setOverrideRenderingPolicy(DescriptorRenderer.OverrideRenderingPolicy.RENDER_OVERRIDE)
                .setUnitReturnType(false);
    }
}
