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

package org.jetbrains.jet.codegen.when;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.codegen.binding.CodegenBinding;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.lang.psi.JetWhenExpression;
import org.jetbrains.org.objectweb.asm.Type;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MappingsClassesForWhenByEnum {
    private final GenerationState state;
    private final Set<String> generatedMappingClasses = new HashSet<String>();
    private final MappingClassesForWhenByEnumCodegen mappingsCodegen;

    public MappingsClassesForWhenByEnum(@NotNull GenerationState state) {
        this.state = state;
        this.mappingsCodegen = new MappingClassesForWhenByEnumCodegen(state);
    }

    public void generateMappingsClassForExpression(@NotNull JetWhenExpression expression) {
        WhenByEnumsMapping mapping = state.getBindingContext().get(CodegenBinding.MAPPING_FOR_WHEN_BY_ENUM, expression);

        assert mapping != null : "mapping class should not be requested for non enum when";

        if (!generatedMappingClasses.contains(mapping.getMappingsClassInternalName())) {
            List<WhenByEnumsMapping> mappings = state.getBindingContext().get(
                    CodegenBinding.MAPPINGS_FOR_WHENS_BY_ENUM_IN_CLASS_FILE,
                    mapping.getOuterClassInternalNameForExpression()
            );

            assert mappings != null : "guaranteed by usage contract of EnumSwitchCodegen";

            Type mappingsClassType = Type.getObjectType(mapping.getMappingsClassInternalName());

            mappingsCodegen.generate(mappings, mappingsClassType, expression.getContainingJetFile());
            generatedMappingClasses.add(mapping.getMappingsClassInternalName());
        }
    }
}
