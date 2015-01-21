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

package org.jetbrains.kotlin.diagnostics.rendering;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.JetClass;
import org.jetbrains.kotlin.psi.JetClassOrObject;
import org.jetbrains.kotlin.psi.JetNamedDeclaration;
import org.jetbrains.kotlin.renderer.DescriptorRenderer;
import org.jetbrains.kotlin.renderer.Renderer;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.calls.inference.*;
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPosition;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.types.*;
import org.jetbrains.kotlin.types.checker.JetTypeChecker;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.jetbrains.kotlin.diagnostics.rendering.TabledDescriptorRenderer.*;
import static org.jetbrains.kotlin.resolve.calls.inference.TypeBounds.BoundKind.LOWER_BOUND;
import static org.jetbrains.kotlin.resolve.calls.inference.TypeBounds.BoundKind.UPPER_BOUND;
import static org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind.RECEIVER_POSITION;
import static org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind.VALUE_PARAMETER_POSITION;

public class Renderers {
    private static final Logger LOG = Logger.getInstance(Renderers.class);

    public static final Renderer<Object> TO_STRING = new Renderer<Object>() {
        @NotNull
        @Override
        public String render(@NotNull Object element) {
            if (element instanceof DeclarationDescriptor) {
                LOG.warn("Diagnostic renderer TO_STRING was used to render an instance of DeclarationDescriptor.\n" +
                         "This is usually a bad idea, because descriptors' toString() includes some debug information, " +
                         "which should not be seen by the user.\nDescriptor: " + element);
            }
            return element.toString();
        }

        @Override
        public String toString() {
            return "TO_STRING";
        }
    };

    public static final Renderer<String> STRING = new Renderer<String>() {
        @NotNull
        @Override
        public String render(@NotNull String element) {
            return element;
        }
    };

    public static final Renderer<Named> NAME = new Renderer<Named>() {
        @NotNull
        @Override
        public String render(@NotNull Named element) {
            return element.getName().asString();
        }
    };

    public static final Renderer<PsiElement> ELEMENT_TEXT = new Renderer<PsiElement>() {
        @NotNull
        @Override
        public String render(@NotNull PsiElement element) {
            return element.getText();
        }
    };

    public static final Renderer<JetNamedDeclaration> DECLARATION_NAME = new Renderer<JetNamedDeclaration>() {
        @NotNull
        @Override
        public String render(@NotNull JetNamedDeclaration element) {
            return element.getNameAsSafeName().asString();
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

    public static final Renderer<ClassDescriptor> RENDER_CLASS_OR_OBJECT_NAME = new Renderer<ClassDescriptor>() {
        @NotNull
        @Override
        public String render(@NotNull ClassDescriptor classifier) {
            return RenderingPackage.renderKindWithName(classifier);
        }
    };

    public static final Renderer<JetType> RENDER_TYPE = new Renderer<JetType>() {
        @NotNull
        @Override
        public String render(@NotNull JetType type) {
            return DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(type);
        }
    };

    public static final Renderer<Variance> RENDER_POSITION_VARIANCE = new Renderer<Variance>() {
        @NotNull
        @Override
        public String render(@NotNull Variance variance) {
            switch (variance) {
                case INVARIANT: return "invariant";
                case IN_VARIANCE: return "in";
                case OUT_VARIANCE: return "out";
            }
            throw new IllegalArgumentException("Unknown variance: " + variance);
        }
    };

    public static final Renderer<Collection<? extends ResolvedCall<?>>> AMBIGUOUS_CALLS =
            new Renderer<Collection<? extends ResolvedCall<?>>>() {
                @NotNull
                @Override
                public String render(@NotNull Collection<? extends ResolvedCall<?>> argument) {
                    StringBuilder stringBuilder = new StringBuilder("\n");
                    for (ResolvedCall<?> call : argument) {
                        stringBuilder.append(DescriptorRenderer.FQ_NAMES_IN_TYPES.render(call.getResultingDescriptor())).append("\n");
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

    public static final Renderer<InferenceErrorData> TYPE_INFERENCE_CANNOT_CAPTURE_TYPES_RENDERER =
            new Renderer<InferenceErrorData>() {
                @NotNull
                @Override
                public String render(@NotNull InferenceErrorData inferenceErrorData) {
                    return renderCannotCaptureTypeParameterError(inferenceErrorData, TabledDescriptorRenderer.create()).toString();
                }
            };

    public static TabledDescriptorRenderer renderConflictingSubstitutionsInferenceError(InferenceErrorData inferenceErrorData,
            TabledDescriptorRenderer result) {
        LOG.assertTrue(inferenceErrorData.constraintSystem.getStatus().hasConflictingConstraints(), renderDebugMessage(
                "Conflicting substitutions inference error renderer is applied for incorrect status", inferenceErrorData));

        Collection<CallableDescriptor> substitutedDescriptors = Lists.newArrayList();
        Collection<TypeSubstitutor> substitutors = ConstraintsUtil.getSubstitutorsForConflictingParameters(
                inferenceErrorData.constraintSystem);
        for (TypeSubstitutor substitutor : substitutors) {
            CallableDescriptor substitutedDescriptor = inferenceErrorData.descriptor.substitute(substitutor);
            substitutedDescriptors.add(substitutedDescriptor);
        }

        TypeParameterDescriptor firstConflictingParameter = ConstraintsUtil.getFirstConflictingParameter(inferenceErrorData.constraintSystem);
        if (firstConflictingParameter == null) {
            LOG.error(renderDebugMessage("There is no conflicting parameter for 'conflicting constraints' error.", inferenceErrorData));
            return result;
        }

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
            JetType receiverType = DescriptorUtils.getReceiverParameterType(substitutedDescriptor.getExtensionReceiverParameter());

            final Collection<ConstraintPosition> errorPositions = Sets.newHashSet();
            List<JetType> parameterTypes = Lists.newArrayList();
            for (ValueParameterDescriptor valueParameterDescriptor : substitutedDescriptor.getValueParameters()) {
                parameterTypes.add(valueParameterDescriptor.getType());
                if (valueParameterDescriptor.getIndex() >= inferenceErrorData.valueArgumentsTypes.size()) continue;
                JetType actualType = inferenceErrorData.valueArgumentsTypes.get(valueParameterDescriptor.getIndex());
                if (!JetTypeChecker.DEFAULT.isSubtypeOf(actualType, valueParameterDescriptor.getType())) {
                    errorPositions.add(VALUE_PARAMETER_POSITION.position(valueParameterDescriptor.getIndex()));
                }
            }

            if (receiverType != null && inferenceErrorData.receiverArgumentType != null &&
                    !JetTypeChecker.DEFAULT.isSubtypeOf(inferenceErrorData.receiverArgumentType, receiverType)) {
                errorPositions.add(RECEIVER_POSITION.position());
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

    @NotNull
    public static TabledDescriptorRenderer renderTypeConstructorMismatchError(
            final @NotNull InferenceErrorData inferenceErrorData,
            @NotNull TabledDescriptorRenderer renderer
    ) {
        Predicate<ConstraintPosition> isErrorPosition = new Predicate<ConstraintPosition>() {
            @Override
            public boolean apply(ConstraintPosition constraintPosition) {
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

    @NotNull
    public static TabledDescriptorRenderer renderNoInformationForParameterError(
            @NotNull InferenceErrorData inferenceErrorData,
            @NotNull TabledDescriptorRenderer result
    ) {
        TypeParameterDescriptor firstUnknownParameter = null;
        for (TypeParameterDescriptor typeParameter : inferenceErrorData.constraintSystem.getTypeVariables()) {
            if (inferenceErrorData.constraintSystem.getTypeBounds(typeParameter).isEmpty()) {
                firstUnknownParameter = typeParameter;
                break;
            }
        }
        if (firstUnknownParameter == null) {
            LOG.error(renderDebugMessage("There is no unknown parameter for 'no information for parameter error'.", inferenceErrorData));
            return result;
        }

        return result
                .text(newText().normal("Not enough information to infer parameter ")
                              .strong(firstUnknownParameter.getName())
                              .normal(" in "))
                .table(newTable()
                               .descriptor(inferenceErrorData.descriptor)
                               .text("Please specify it explicitly."));
    }

    @NotNull
    public static TabledDescriptorRenderer renderUpperBoundViolatedInferenceError(InferenceErrorData inferenceErrorData, TabledDescriptorRenderer result) {
        TypeParameterDescriptor typeParameterDescriptor = null;
        ConstraintSystemImpl constraintSystem = (ConstraintSystemImpl) inferenceErrorData.constraintSystem;
        ConstraintSystemStatus status = constraintSystem.getStatus();
        LOG.assertTrue(status.hasViolatedUpperBound(), renderDebugMessage(
                "Upper bound violated renderer is applied for incorrect status", inferenceErrorData));

        ConstraintSystem systemWithoutWeakConstraints = constraintSystem.getSystemWithoutWeakConstraints();
        for (TypeParameterDescriptor typeParameter : inferenceErrorData.descriptor.getTypeParameters()) {
            if (!ConstraintsUtil.checkUpperBoundIsSatisfied(systemWithoutWeakConstraints, typeParameter, true)) {
                typeParameterDescriptor = typeParameter;
            }
        }
        if (typeParameterDescriptor == null && status.hasConflictingConstraints()) {
            return renderConflictingSubstitutionsInferenceError(inferenceErrorData, result);
        }
        if (typeParameterDescriptor == null) {
            LOG.error(renderDebugMessage("There is no type parameter with violated upper bound for 'upper bound violated' error",
                                         inferenceErrorData));
            return result;
        }

        JetType inferredValueForTypeParameter = systemWithoutWeakConstraints.getTypeBounds(typeParameterDescriptor).getValue();
        if (inferredValueForTypeParameter == null) {
            LOG.error(renderDebugMessage("System without weak constraints is not successful, there is no value for type parameter " +
                                         typeParameterDescriptor.getName() + "\n: " + systemWithoutWeakConstraints, inferenceErrorData));
            return result;
        }

        result.text(newText().normal("Type parameter bound for ").strong(typeParameterDescriptor.getName()).normal(" in "))
                .table(newTable().
                        descriptor(inferenceErrorData.descriptor));

        JetType violatedUpperBound = null;
        for (JetType upperBound : typeParameterDescriptor.getUpperBounds()) {
            JetType upperBoundWithSubstitutedInferredTypes =
                    systemWithoutWeakConstraints.getResultingSubstitutor().substitute(upperBound, Variance.INVARIANT);
            if (upperBoundWithSubstitutedInferredTypes != null &&
                !JetTypeChecker.DEFAULT.isSubtypeOf(inferredValueForTypeParameter, upperBoundWithSubstitutedInferredTypes)) {
                violatedUpperBound = upperBoundWithSubstitutedInferredTypes;
                break;
            }
        }
        if (violatedUpperBound == null) {
            LOG.error(renderDebugMessage("Type parameter (chosen as violating its upper bound)" + typeParameterDescriptor.getName() +
                                         " violates no bounds after substitution", inferenceErrorData));
            return result;
        }

        Renderer<JetType> typeRenderer = result.getTypeRenderer();
        result.text(newText()
                            .normal(" is not satisfied: inferred type ")
                            .error(typeRenderer.render(inferredValueForTypeParameter))
                            .normal(" is not a subtype of ")
                            .strong(typeRenderer.render(violatedUpperBound)));
        return result;
    }

    @NotNull
    public static TabledDescriptorRenderer renderCannotCaptureTypeParameterError(
            @NotNull InferenceErrorData inferenceErrorData,
            @NotNull TabledDescriptorRenderer result
    ) {
        ConstraintSystemImpl constraintSystem = (ConstraintSystemImpl) inferenceErrorData.constraintSystem;
        List<ConstraintError> errors = constraintSystem.getConstraintErrors();
        TypeParameterDescriptor typeParameterWithCapturedConstraint = null;
        for (ConstraintError error : errors) {
            if (error instanceof CannotCapture) {
                typeParameterWithCapturedConstraint = ((CannotCapture) error).getTypeVariable();
            }
        }
        if (typeParameterWithCapturedConstraint == null) {
            LOG.error(renderDebugMessage("An error 'cannot capture type parameter' is not found in errors", inferenceErrorData));
            return result;
        }

        CapturedTypeConstructor capturedTypeConstructor = null;
        TypeBounds typeBounds = constraintSystem.getTypeBounds(typeParameterWithCapturedConstraint);
            for (TypeBounds.Bound bound : typeBounds.getBounds()) {
                TypeConstructor constructor = bound.getConstrainingType().getConstructor();
                if (constructor instanceof CapturedTypeConstructor) {
                    capturedTypeConstructor = (CapturedTypeConstructor) constructor;
                }
        }
        if (capturedTypeConstructor == null) {
            LOG.error(renderDebugMessage("There is no captured type in bounds, but there is an error 'cannot capture type parameter'",
                                         inferenceErrorData));
            return result;
        }

        String explanation;
        JetType upperBound = typeParameterWithCapturedConstraint.getUpperBoundsAsType();
        if (!KotlinBuiltIns.isNullableAny(upperBound)
            && capturedTypeConstructor.getTypeProjection().getProjectionKind() == Variance.IN_VARIANCE) {
            explanation = "Type parameter has an upper bound '" + result.getTypeRenderer().render(upperBound) + "'" +
                           " that cannot be satisfied capturing 'in' projection";
        }
        else {
            explanation = "Only top-level type projections can be captured";
        }
        result.text(newText().normal("'" + typeParameterWithCapturedConstraint.getName() + "'" +
                                     " cannot capture " +
                                     "'" + capturedTypeConstructor.getTypeProjection() + "'. " +
                                     explanation));
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

    public static final Renderer<ConstraintSystem> RENDER_CONSTRAINT_SYSTEM = new Renderer<ConstraintSystem>() {
        @NotNull
        @Override
        public String render(@NotNull ConstraintSystem constraintSystem) {
            Set<TypeParameterDescriptor> typeVariables = constraintSystem.getTypeVariables();
            Set<TypeBounds> typeBounds = Sets.newLinkedHashSet();
            for (TypeParameterDescriptor variable : typeVariables) {
                typeBounds.add(constraintSystem.getTypeBounds(variable));
            }
            Function<TypeBounds, String> renderTypeBounds = rendererToFunction(RENDER_TYPE_BOUNDS);
            return "type parameter bounds:\n" + StringUtil.join(typeBounds, renderTypeBounds, "\n") + "\n" +
                   "status:\n" + ConstraintsUtil.getDebugMessageForStatus(constraintSystem.getStatus());
        }
    };

    public static final Renderer<TypeBounds> RENDER_TYPE_BOUNDS = new Renderer<TypeBounds>() {
        @NotNull
        @Override
        public String render(@NotNull TypeBounds typeBounds) {
            Function<TypeBoundsImpl.Bound, String> renderBound = new Function<TypeBoundsImpl.Bound, String>() {
                @Override
                public String fun(TypeBoundsImpl.Bound bound) {
                    String arrow = bound.getKind() == LOWER_BOUND ? ">: " : bound.getKind() == UPPER_BOUND ? "<: " : ":= ";
                    return arrow + RENDER_TYPE.render(bound.getConstrainingType()) + '(' + bound.getPosition() + ')';
                }
            };
            Name typeVariableName = typeBounds.getTypeVariable().getName();
            if (typeBounds.isEmpty()) {
                return typeVariableName.asString();
            }
            return typeVariableName + " " + StringUtil.join(typeBounds.getBounds(), renderBound, ", ");
        }
    };

    @NotNull
    public static <T> Function<T, String> rendererToFunction(final @NotNull Renderer<T> renderer) {
        return new Function<T, String>() {
            @Override
            public String fun(T t) {
                return renderer.render(t);
            }
        };
    }

    @NotNull
    private static String renderDebugMessage(String message, InferenceErrorData inferenceErrorData) {
        StringBuilder result = new StringBuilder();
        result.append(message);
        result.append("\nConstraint system: \n");
        result.append(RENDER_CONSTRAINT_SYSTEM.render(inferenceErrorData.constraintSystem));
        result.append("\nDescriptor:\n");
        result.append(inferenceErrorData.descriptor);
        result.append("\nExpected type:\n");
        if (TypeUtils.noExpectedType(inferenceErrorData.expectedType)) {
            result.append(inferenceErrorData.expectedType);
        }
        else {
            result.append(RENDER_TYPE.render(inferenceErrorData.expectedType));
        }
        result.append("\nArgument types:\n");
        if (inferenceErrorData.receiverArgumentType != null) {
            result.append(RENDER_TYPE.render(inferenceErrorData.receiverArgumentType)).append(".");
        }
        result.append("(").append(StringUtil.join(inferenceErrorData.valueArgumentsTypes, new Function<JetType, String>() {
            @Override
            public String fun(JetType type) {
                return RENDER_TYPE.render(type);
            }
        }, ", ")).append(")");
        return result.toString();
    }

    private Renderers() {
    }
}
