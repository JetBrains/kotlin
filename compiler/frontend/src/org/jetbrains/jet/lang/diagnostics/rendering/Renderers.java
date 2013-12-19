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

package org.jetbrains.jet.lang.diagnostics.rendering;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.calls.inference.*;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeSubstitutor;
import org.jetbrains.jet.lang.types.Variance;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.renderer.DescriptorRenderer;
import org.jetbrains.jet.renderer.Renderer;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static org.jetbrains.jet.lang.diagnostics.rendering.TabledDescriptorRenderer.*;

public class Renderers {
    public static final Renderer<Object> TO_STRING = new Renderer<Object>() {
        @NotNull
        @Override
        public String render(@NotNull Object element) {
            return element.toString();
        }

        @Override
        public String toString() {
            return "TO_STRING";
        }
    };

    public static final Renderer<Object> NAME = new Renderer<Object>() {
        @NotNull
        @Override
        public String render(@NotNull Object element) {
            if (element instanceof Named) {
                return ((Named) element).getName().asString();
            }
            return element.toString();
        }
    };

    public static final Renderer<PsiElement> ELEMENT_TEXT = new Renderer<PsiElement>() {
        @NotNull
        @Override
        public String render(@NotNull PsiElement element) {
            return element.getText();
        }
    };
    
    public static final Renderer<JetClassOrObject> RENDER_CLASS_OR_OBJECT = new Renderer<JetClassOrObject>() {
        @NotNull
        @Override
        public String render(@NotNull JetClassOrObject classOrObject) {
            String name = classOrObject.getName() != null ? " '" + classOrObject.getName() + "'" : "";
            if (classOrObject instanceof JetClass) {
                return "Class" + name;
            }
            return "Object" + name;

        }
    };

    public static final Renderer<JetType> RENDER_TYPE = new Renderer<JetType>() {
        @NotNull
        @Override
        public String render(@NotNull JetType type) {
            return DescriptorRenderer.TEXT.renderType(type);
        }
    };

    public static final Renderer<Collection<? extends ResolvedCall<?>>> AMBIGUOUS_CALLS =
            new Renderer<Collection<? extends ResolvedCall<? extends CallableDescriptor>>>() {
                @NotNull
                @Override
                public String render(@NotNull Collection<? extends ResolvedCall<? extends CallableDescriptor>> argument) {
                    StringBuilder stringBuilder = new StringBuilder("\n");
                    for (ResolvedCall<? extends CallableDescriptor> call : argument) {
                        stringBuilder.append(DescriptorRenderer.TEXT.render(call.getResultingDescriptor())).append("\n");
                    }
                    return stringBuilder.toString();
                }
            };

    public static <T> Renderer<Collection<? extends T>> commaSeparated(final Renderer<T> itemRenderer) {
        return new Renderer<Collection<? extends T>>() {
            @NotNull
            @Override
            public String render(@NotNull Collection<? extends T> object) {
                StringBuilder result = new StringBuilder();
                for (Iterator<? extends T> iterator = object.iterator(); iterator.hasNext(); ) {
                    T next = iterator.next();
                    result.append(itemRenderer.render(next));
                    if (iterator.hasNext()) {
                        result.append(", ");
                    }
                }
                return result.toString();
            }
        };
    }

    public static final Renderer<InferenceErrorData> TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS_RENDERER =
            new Renderer<InferenceErrorData>() {
                @NotNull
                @Override
                public String render(@NotNull InferenceErrorData inferenceErrorData) {
                    return renderConflictingSubstitutionsInferenceError(inferenceErrorData, TabledDescriptorRenderer.create()).toString();
                }
            };

    public static final Renderer<InferenceErrorData> TYPE_INFERENCE_TYPE_CONSTRUCTOR_MISMATCH_RENDERER =
            new Renderer<InferenceErrorData>() {
                @NotNull
                @Override
                public String render(@NotNull InferenceErrorData inferenceErrorData) {
                    return renderTypeConstructorMismatchError(inferenceErrorData, TabledDescriptorRenderer.create()).toString();
                }
            };

    public static final Renderer<InferenceErrorData> TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER_RENDERER =
            new Renderer<InferenceErrorData>() {
                @NotNull
                @Override
                public String render(@NotNull InferenceErrorData inferenceErrorData) {
                    return renderNoInformationForParameterError(inferenceErrorData, TabledDescriptorRenderer.create()).toString();
                }
            };

    public static final Renderer<InferenceErrorData> TYPE_INFERENCE_UPPER_BOUND_VIOLATED_RENDERER =
            new Renderer<InferenceErrorData>() {
                @NotNull
                @Override
                public String render(@NotNull InferenceErrorData inferenceErrorData) {
                    return renderUpperBoundViolatedInferenceError(inferenceErrorData, TabledDescriptorRenderer.create()).toString();
                }
            };

    public static TabledDescriptorRenderer renderConflictingSubstitutionsInferenceError(InferenceErrorData inferenceErrorData,
            TabledDescriptorRenderer result) {
        assert inferenceErrorData.constraintSystem.getStatus().hasConflictingConstraints();

        Collection<CallableDescriptor> substitutedDescriptors = Lists.newArrayList();
        Collection<TypeSubstitutor> substitutors = ConstraintsUtil.getSubstitutorsForConflictingParameters(
                inferenceErrorData.constraintSystem);
        for (TypeSubstitutor substitutor : substitutors) {
            CallableDescriptor substitutedDescriptor = inferenceErrorData.descriptor.substitute(substitutor);
            substitutedDescriptors.add(substitutedDescriptor);
        }

        TypeParameterDescriptor firstConflictingParameter = ConstraintsUtil.getFirstConflictingParameter(inferenceErrorData.constraintSystem);
        assert firstConflictingParameter != null;

        result.text(newText()
                            .normal("Cannot infer type parameter ")
                            .strong(firstConflictingParameter.getName())
                            .normal(" in "));
        //String type = strong(firstConflictingParameter.getName());
        TableRenderer table = newTable();
        result.table(table);
        table.descriptor(inferenceErrorData.descriptor)
             .text("None of the following substitutions");

        for (CallableDescriptor substitutedDescriptor : substitutedDescriptors) {
            JetType receiverType = DescriptorUtils.getReceiverParameterType(substitutedDescriptor.getReceiverParameter());

            final Collection<ConstraintPosition> errorPositions = Sets.newHashSet();
            List<JetType> parameterTypes = Lists.newArrayList();
            for (ValueParameterDescriptor valueParameterDescriptor : substitutedDescriptor.getValueParameters()) {
                parameterTypes.add(valueParameterDescriptor.getType());
                if (valueParameterDescriptor.getIndex() >= inferenceErrorData.valueArgumentsTypes.size()) continue;
                JetType actualType = inferenceErrorData.valueArgumentsTypes.get(valueParameterDescriptor.getIndex());
                if (!JetTypeChecker.INSTANCE.isSubtypeOf(actualType, valueParameterDescriptor.getType())) {
                    errorPositions.add(ConstraintPosition.getValueParameterPosition(valueParameterDescriptor.getIndex()));
                }
            }

            if (receiverType != null && inferenceErrorData.receiverArgumentType != null &&
                    !JetTypeChecker.INSTANCE.isSubtypeOf(inferenceErrorData.receiverArgumentType, receiverType)) {
                errorPositions.add(ConstraintPosition.RECEIVER_POSITION);
            }

            Predicate<ConstraintPosition> isErrorPosition = new Predicate<ConstraintPosition>() {
                @Override
                public boolean apply(@Nullable ConstraintPosition constraintPosition) {
                    return errorPositions.contains(constraintPosition);
                }
            };
            table.functionArgumentTypeList(receiverType, parameterTypes, isErrorPosition);
        }

        table.text("can be applied to")
                .functionArgumentTypeList(inferenceErrorData.receiverArgumentType, inferenceErrorData.valueArgumentsTypes);

        return result;
    }

    public static TabledDescriptorRenderer renderTypeConstructorMismatchError(final InferenceErrorData inferenceErrorData,
            TabledDescriptorRenderer renderer) {
        Predicate<ConstraintPosition> isErrorPosition = new Predicate<ConstraintPosition>() {
            @Override
            public boolean apply(@Nullable ConstraintPosition constraintPosition) {
                assert constraintPosition != null;
                return inferenceErrorData.constraintSystem.getStatus().hasTypeConstructorMismatchAt(constraintPosition);
            }
        };
        return renderer.table(TabledDescriptorRenderer.newTable()
                                      .descriptor(inferenceErrorData.descriptor)
                                      .text("cannot be applied to")
                                      .functionArgumentTypeList(
                                              inferenceErrorData.receiverArgumentType,
                                              inferenceErrorData.valueArgumentsTypes,
                                              isErrorPosition));
    }

    public static TabledDescriptorRenderer renderNoInformationForParameterError(InferenceErrorData inferenceErrorData,
            TabledDescriptorRenderer renderer) {
        TypeParameterDescriptor firstUnknownParameter = null;
        for (TypeParameterDescriptor typeParameter : inferenceErrorData.constraintSystem.getTypeVariables()) {
            if (inferenceErrorData.constraintSystem.getTypeBounds(typeParameter).isEmpty()) {
                firstUnknownParameter = typeParameter;
                break;
            }
        }
        assert firstUnknownParameter != null;

        return renderer
                .text(newText().normal("Not enough information to infer parameter ")
                              .strong(firstUnknownParameter.getName())
                              .normal(" in "))
                .table(newTable()
                               .descriptor(inferenceErrorData.descriptor)
                               .text("Please specify it explicitly."));
    }

    @NotNull
    public static TabledDescriptorRenderer renderUpperBoundViolatedInferenceError(InferenceErrorData inferenceErrorData, TabledDescriptorRenderer result) {
        String errorMessage = "Rendering 'upper bound violated' error for " + inferenceErrorData.descriptor;

        TypeParameterDescriptor typeParameterDescriptor = null;
        ConstraintSystemImpl constraintSystem = (ConstraintSystemImpl) inferenceErrorData.constraintSystem;
        assert constraintSystem.getStatus().hasViolatedUpperBound();

        ConstraintSystem systemWithoutWeakConstraints = constraintSystem.getSystemWithoutWeakConstraints();
        for (TypeParameterDescriptor typeParameter : inferenceErrorData.descriptor.getTypeParameters()) {
            if (!ConstraintsUtil.checkUpperBoundIsSatisfied(systemWithoutWeakConstraints, typeParameter, true)) {
                typeParameterDescriptor = typeParameter;
            }
        }
        assert typeParameterDescriptor != null : errorMessage;

        JetType inferredValueForTypeParameter = systemWithoutWeakConstraints.getTypeBounds(typeParameterDescriptor).getValue();
        assert inferredValueForTypeParameter != null : errorMessage;

        result.text(newText().normal("Type parameter bound for ").strong(typeParameterDescriptor.getName()).normal(" in "))
                .table(newTable().
                        descriptor(inferenceErrorData.descriptor));

        JetType violatedUpperBound = null;
        for (JetType upperBound : typeParameterDescriptor.getUpperBounds()) {
            JetType upperBoundWithSubstitutedInferredTypes =
                    systemWithoutWeakConstraints.getResultingSubstitutor().substitute(upperBound, Variance.INVARIANT);
            if (upperBoundWithSubstitutedInferredTypes != null &&
                !JetTypeChecker.INSTANCE.isSubtypeOf(inferredValueForTypeParameter, upperBoundWithSubstitutedInferredTypes)) {
                violatedUpperBound = upperBoundWithSubstitutedInferredTypes;
                break;
            }
        }
        assert violatedUpperBound != null : errorMessage;

        Renderer<JetType> typeRenderer = result.getTypeRenderer();
        result.text(newText()
                            .normal(" is not satisfied: inferred type ")
                            .error(typeRenderer.render(inferredValueForTypeParameter))
                            .normal(" is not a subtype of ")
                            .strong(typeRenderer.render(violatedUpperBound)));
        return result;
    }

    public static final Renderer<Collection<ClassDescriptor>> CLASSES_OR_SEPARATED = new Renderer<Collection<ClassDescriptor>>() {
        @NotNull
        @Override
        public String render(@NotNull Collection<ClassDescriptor> descriptors) {
            StringBuilder sb = new StringBuilder();
            int index = 0;
            for (ClassDescriptor descriptor : descriptors) {
                sb.append(DescriptorUtils.getFqName(descriptor).asString());
                index++;
                if (index <= descriptors.size() - 2) {
                    sb.append(", ");
                }
                else if (index == descriptors.size() - 1) {
                    sb.append(" or ");
                }
            }
            return sb.toString();
        }
    };

    public static final Renderer<Collection<JetType>> RENDER_COLLECTION_OF_TYPES = new Renderer<Collection<JetType>>() {
        @NotNull
        @Override
        public String render(@NotNull Collection<JetType> types) {
            return StringUtil.join(types, new Function<JetType, String>() {
                @Override
                public String fun(JetType type) {
                    return RENDER_TYPE.render(type);
                }
            }, ", ");
        }
    };


    private Renderers() {
    }
}
