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

package org.jetbrains.kotlin.types.expressions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor;
import org.jetbrains.kotlin.psi.JetTypeReference;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.PossiblyBareType;
import org.jetbrains.kotlin.types.*;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;

import static org.jetbrains.jet.lang.diagnostics.Errors.NO_TYPE_ARGUMENTS_ON_RHS;

public class TypeReconstructionUtil {
    @NotNull
    public static JetType reconstructBareType(
            @NotNull JetTypeReference right,
            @NotNull PossiblyBareType possiblyBareTarget,
            @Nullable JetType subjectType,
            @NotNull BindingTrace trace
    ) {
        if (subjectType == null) {
            // Recovery: let's reconstruct as if we were casting from Any, to get some type there
            subjectType = KotlinBuiltIns.getInstance().getAnyType();
        }
        TypeReconstructionResult reconstructionResult = possiblyBareTarget.reconstruct(subjectType);
        if (!reconstructionResult.isAllArgumentsInferred()) {
            TypeConstructor typeConstructor = possiblyBareTarget.getBareTypeConstructor();
            trace.report(NO_TYPE_ARGUMENTS_ON_RHS.on(right,
                                                     typeConstructor.getParameters().size(),
                                                     allStarProjectionsString(typeConstructor)));
        }

        JetType targetType = reconstructionResult.getResultingType();
        if (targetType != null) {
            if (possiblyBareTarget.isBare()) {
                trace.record(BindingContext.TYPE, right, targetType);
            }
            return targetType;
        }

        return ErrorUtils.createErrorType("Failed to reconstruct type: " + right.getText());
    }

    @NotNull
    private static String allStarProjectionsString(@NotNull TypeConstructor constructor) {
        int size = constructor.getParameters().size();
        assert size != 0 : "No projections possible for a nilary type constructor" + constructor;
        ClassifierDescriptor declarationDescriptor = constructor.getDeclarationDescriptor();
        assert declarationDescriptor != null : "No declaration descriptor for type constructor " + constructor;
        String name = declarationDescriptor.getName().asString();

        return getTypeNameAndStarProjectionsString(name, size);
    }

    @NotNull
    public static String getTypeNameAndStarProjectionsString(@NotNull String name, int size) {
        StringBuilder builder = new StringBuilder(name);
        builder.append("<");
        for (int i = 0; i < size; i++) {
            builder.append("*");
            if (i == size - 1) break;
            builder.append(", ");
        }
        builder.append(">");

        return builder.toString();
    }
}
