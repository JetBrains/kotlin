package org.jetbrains.jet.lang.types;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.*;

/**
 * @author abreslav
 */
public class JetTypeInferrer {

    private final BindingTrace trace;
    private final JetSemanticServices semanticServices;

    public JetTypeInferrer(BindingTrace trace, JetSemanticServices semanticServices) {
        this.trace = trace;
        this.semanticServices = semanticServices;
    }

    /*
      : "new" constructorInvocation

      : objectLiteral

      : SimpleName

      : "typeof" "(" expression ")"

      : functionLiteral

      : declaration
      : "namespace" // for the root namespace
    */
    public Type getType(@NotNull final JetScope scope, @NotNull JetExpression expression, final boolean preferBlock) {
        final Type[] result = new Type[1];
        expression.accept(new JetVisitor() {
            @Override
            public void visitReferenceExpression(JetReferenceExpression expression) {
                // TODO : other members
                // TODO : type substitutions???
                PropertyDescriptor property = scope.getProperty(expression.getReferencedName());
                trace.recordResolutionResult(expression, property);
                if (property != null) {
                    result[0] = property.getType();
                }
            }

            @Override
            public void visitFunctionLiteralExpression(JetFunctionLiteralExpression expression) {
                if (preferBlock && !expression.hasParameterSpecification()) {
                    result[0] = getBlockReturnedType(scope, expression.getBody());
                    return;
                }

                JetTypeReference returnTypeRef = expression.getReturnTypeRef();

                JetTypeReference receiverTypeRef = expression.getReceiverTypeRef();
                final Type receiverType;
                if (receiverTypeRef != null) {
                    receiverType = TypeResolver.INSTANCE.resolveType(scope, receiverTypeRef);
                } else {
                    receiverType = scope.getThisType();
                }

                List<JetElement> body = expression.getBody();
                final Map<String, PropertyDescriptor> parameterDescriptors = new HashMap<String, PropertyDescriptor>();
                List<Type> parameterTypes = new ArrayList<Type>();
                for (JetParameter parameter : expression.getParameters()) {
                    JetTypeReference typeReference = parameter.getTypeReference();
                    if (typeReference == null) {
                        throw new UnsupportedOperationException("Type inference for parameters is not implemented yet");
                    }
                    PropertyDescriptor propertyDescriptor = semanticServices.getClassDescriptorResolver().resolvePropertyDescriptor(scope, parameter);
                    parameterDescriptors.put(parameter.getName(), propertyDescriptor);
                    parameterTypes.add(propertyDescriptor.getType());
                }
                Type returnType;
                if (returnTypeRef != null) {
                    returnType = TypeResolver.INSTANCE.resolveType(scope, returnTypeRef);
                } else {
                    WritableScope writableScope = new WritableScope(scope);
                    for (PropertyDescriptor propertyDescriptor : parameterDescriptors.values()) {
                        writableScope.addPropertyDescriptor(propertyDescriptor);
                    }
                    writableScope.setThisType(receiverType);
                    returnType = getBlockReturnedType(writableScope, body);
                }
                result[0] = JetStandardClasses.getFunctionType(null, receiverTypeRef == null ? null : receiverType, parameterTypes, returnType);
            }

            @Override
            public void visitParenthesizedExpression(JetParenthesizedExpression expression) {
                result[0] = getType(scope, expression.getExpression(), false);
            }

            @Override
            public void visitConstantExpression(JetConstantExpression expression) {
                IElementType elementType = expression.getNode().getElementType();
                JetStandardLibrary standardLibrary = semanticServices.getStandardLibrary();
                if (elementType == JetNodeTypes.INTEGER_CONSTANT) {
                    result[0] = standardLibrary.getIntType();
                } else if (elementType == JetNodeTypes.LONG_CONSTANT) {
                    result[0] = standardLibrary.getLongType();
                } else if (elementType == JetNodeTypes.FLOAT_CONSTANT) {
                    String text = expression.getText();
                    assert text.length() > 0;
                    char lastChar = text.charAt(text.length() - 1);
                    if (lastChar == 'f' || lastChar == 'F') {
                        result[0] = standardLibrary.getFloatType();
                    } else {
                        result[0] = standardLibrary.getDoubleType();
                    }
                } else if (elementType == JetNodeTypes.BOOLEAN_CONSTANT) {
                    result[0] = standardLibrary.getBooleanType();
                } else if (elementType == JetNodeTypes.CHARACTER_CONSTANT) {
                    result[0] = standardLibrary.getCharType();
                } else if (elementType == JetNodeTypes.STRING_CONSTANT) {
                    result[0] = standardLibrary.getStringType();
                } else if (elementType == JetNodeTypes.NULL) {
                    result[0] = JetStandardClasses.getNullableNothingType();
                } else {
                    throw new IllegalArgumentException("Unsupported constant: " + expression);
                }
            }

            @Override
            public void visitThrowExpression(JetThrowExpression expression) {
                result[0] = JetStandardClasses.getNothingType();
            }

            @Override
            public void visitReturnExpression(JetReturnExpression expression) {
                result[0] = JetStandardClasses.getNothingType();
            }

            @Override
            public void visitBreakExpression(JetBreakExpression expression) {
                result[0] = JetStandardClasses.getNothingType();
            }

            @Override
            public void visitContinueExpression(JetContinueExpression expression) {
                result[0] = JetStandardClasses.getNothingType();
            }

            @Override
            public void visitTypeofExpression(JetTypeofExpression expression) {
                throw new UnsupportedOperationException("Return some reflection interface"); // TODO
            }

            @Override
            public void visitBinaryWithTypeRHSExpression(JetBinaryExpressionWithTypeRHS expression) {
                if (expression.getOperationSign() == JetTokens.COLON) {
                    Type actualType = getType(scope, expression.getLeft(), false);
                    Type expectedType = TypeResolver.INSTANCE.resolveType(scope, expression.getRight());
                    if (JetTypeChecker.INSTANCE.isSubtypeOf(actualType, expectedType)) {
                        result[0] = expectedType;
                        return;
                    } else {
                         // TODO
                        throw new UnsupportedOperationException("Type mismatch: expected " + expectedType + " but found " + actualType);
                    }
                }
                throw new UnsupportedOperationException(); // TODO
            }

            @Override
            public void visitIfExpression(JetIfExpression expression) {
                // TODO : check condition type
                // TODO : change types according to is and nullability checks
                JetExpression elseBranch = expression.getElse();
                if (elseBranch == null) {
                    // TODO : type-check the branch
                    result[0] = JetStandardClasses.getUnitType();
                } else {
                    Type thenType = getType(scope, expression.getThen(), true);
                    Type elseType = getType(scope, elseBranch, true);
                    result[0] = JetTypeChecker.INSTANCE.commonSupertype(Arrays.asList(thenType, elseType));
                }
            }

            @Override
            public void visitWhenExpression(JetWhenExpression expression) {
                // TODO :change scope according to the bound value in the when header
                List<Type> expressions = new ArrayList<Type>();
                collectAllReturnTypes(expression, scope, expressions);
                result[0] = JetTypeChecker.INSTANCE.commonSupertype(expressions);
            }

            @Override
            public void visitTryExpression(JetTryExpression expression) {
                JetExpression tryBlock = expression.getTryBlock();
                List<JetCatchClause> catchClauses = expression.getCatchClauses();
                JetFinallySection finallyBlock = expression.getFinallyBlock();
                List<Type> types = new ArrayList<Type>();
                if (finallyBlock == null) {
                    for (JetCatchClause catchClause : catchClauses) {
                        // TODO: change scope here
                        types.add(getType(scope, catchClause.getCatchBody(), true));
                    }
                } else {
                    types.add(getType(scope, finallyBlock.getFinalExpression(), true));
                }
                types.add(getType(scope, tryBlock, true));
                result[0] = JetTypeChecker.INSTANCE.commonSupertype(types);
            }

            @Override
            public void visitTupleExpression(JetTupleExpression expression) {
                List<JetExpression> entries = expression.getEntries();
                List<Type> types = new ArrayList<Type>();
                for (JetExpression entry : entries) {
                    types.add(getType(scope, entry, false));
                }
                // TODO : labels
                result[0] = JetStandardClasses.getTupleType(types);
            }

            @Override
            public void visitThisExpression(JetThisExpression expression) {
                // TODO : qualified this, e.g. Foo.this<Bar>
                Type thisType = scope.getThisType();
                JetTypeReference superTypeQualifier = expression.getSuperTypeQualifier();
                if (superTypeQualifier != null) {
                    // This cast must be safe (assuming the PSI doesn't contain errors)
                    JetUserType typeElement = (JetUserType) superTypeQualifier.getTypeElement();
                    ClassDescriptor superclass = TypeResolver.INSTANCE.resolveClass(scope, typeElement);
                    Collection<? extends Type> supertypes = thisType.getConstructor().getSupertypes();
                    Map<TypeConstructor, TypeProjection> substitutionContext = TypeSubstitutor.INSTANCE.getSubstitutionContext(thisType);
                    for (Type declaredSupertype : supertypes) {
                        if (declaredSupertype.getConstructor().equals(superclass.getTypeConstructor())) {
                            result[0] = TypeSubstitutor.INSTANCE.substitute(substitutionContext, declaredSupertype, Variance.INVARIANT);
                            break;
                        }
                    }
                    assert result[0] != null;
                } else {
                    result[0] = thisType;
                }
            }

            @Override
            public void visitBlockExpression(JetBlockExpression expression) {
                result[0] = getBlockReturnedType(scope, expression.getStatements());
            }

            @Override
            public void visitLoopExpression(JetLoopExpression expression) {
                result[0] = JetStandardClasses.getUnitType();
            }

            @Override
            public void visitNewExpression(JetNewExpression expression) {
                // TODO : type argument inference
                JetTypeReference typeReference = expression.getTypeReference();
                result[0] = TypeResolver.INSTANCE.resolveType(scope, typeReference);
            }

            @Override
            public void visitDotQualifiedExpression(JetDotQualifiedExpression expression) {
                // TODO : functions
                JetExpression receiverExpression = expression.getReceiverExpression();
                JetExpression selectorExpression = expression.getSelectorExpression();
                Type receiverType = getType(scope, receiverExpression, false);
                JetScope compositeScope = new ScopeWithReceiver(scope, receiverType);
                result[0] = getType(compositeScope, selectorExpression, false);
            }

            @Override
            public void visitCallExpression(JetCallExpression expression) {
                JetExpression calleeExpression = expression.getCalleeExpression();

                // 1) ends with a name -> (scope, name) to look up
                // 2) ends with something else -> just check types

                // TODO : check somewhere that these are NOT projections
                List<JetTypeProjection> typeArguments = expression.getTypeArguments();

                List<JetArgument> valueArguments = expression.getValueArguments();
                boolean someNamed = false;
                for (JetArgument argument : valueArguments) {
                    if (argument.isNamed()) {
                        someNamed = true;
                        break;
                    }
                }
                List<JetExpression> functionLiteralArguments = expression.getFunctionLiteralArguments();

//                JetExpression functionLiteralArgument = functionLiteralArguments.isEmpty() ? null : functionLiteralArguments.get(0);
                // TODO : must be a check
                assert functionLiteralArguments.size() <= 1;

                OverloadDomain overloadDomain = getOverloadDomain(scope, calleeExpression);
                if (someNamed) {
                    // TODO : check that all are named
                    throw new UnsupportedOperationException(); // TODO

//                    result[0] = overloadDomain.getFunctionDescriptorForNamedArguments(typeArguments, valueArguments, functionLiteralArgument);
                } else {
                    List<JetExpression> positionedValueArguments = new ArrayList<JetExpression>();
                    for (JetArgument argument : valueArguments) {
                        positionedValueArguments.add(argument.getArgumentExpression());
                    }

                    positionedValueArguments.addAll(functionLiteralArguments);

                    List<Type> types = new ArrayList<Type>();
                    for (JetTypeProjection projection : typeArguments) {
                        // TODO : check that there's no projection
                        types.add(TypeResolver.INSTANCE.resolveType(scope, projection.getTypeReference()));
                    }

                    List<Type> valueArgumentTypes = new ArrayList<Type>();
                    for (JetExpression valueArgument : positionedValueArguments) {
                        valueArgumentTypes.add(getType(scope, valueArgument, false));
                    }

                    FunctionDescriptor functionDescriptor = overloadDomain.getFunctionDescriptorForPositionedArguments(types, valueArgumentTypes);
                    if (functionDescriptor != null) {
                        result[0] = functionDescriptor.getUnsubstitutedReturnType();
                    }
                }
            }

            @Override
            public void visitJetElement(JetElement elem) {
                throw new IllegalArgumentException("Unsupported element: " + elem);
            }
        });
        trace.recordExpressionType(expression, result[0]);
        return result[0];
    }

    private OverloadDomain getOverloadDomain(final JetScope scope, JetExpression calleeExpression) {
        final OverloadDomain[] result = new OverloadDomain[1];
        final JetReferenceExpression[] reference = new JetReferenceExpression[1];
        calleeExpression.accept(new JetVisitor() {

            @Override
            public void visitHashQualifiedExpression(JetHashQualifiedExpression expression) {
                // a#b -- create a domain for all overloads of b in a
                throw new UnsupportedOperationException(); // TODO
            }

            @Override
            public void visitPredicateExpression(JetPredicateExpression expression) {
                // overload lookup for checking, but the type is receiver's type + nullable
                throw new UnsupportedOperationException(); // TODO
            }

            @Override
            public void visitQualifiedExpression(JetQualifiedExpression expression) {
                // . or ?.
                JetExpression selectorExpression = expression.getSelectorExpression();
                if (selectorExpression instanceof JetReferenceExpression) {
                    JetReferenceExpression referenceExpression = (JetReferenceExpression) selectorExpression;

                    Type receiverType = getType(scope, expression.getReceiverExpression(), false);
                    result[0] = OverloadResolver.INSTANCE.getOverloadDomain(receiverType, scope, referenceExpression.getReferencedName());
                    reference[0] = referenceExpression;
                } else {
                    throw new UnsupportedOperationException(); // TODO
                }
            }

            @Override
            public void visitReferenceExpression(JetReferenceExpression expression) {
                // a -- create a hierarchical lookup domain for this.a
                result[0] = OverloadResolver.INSTANCE.getOverloadDomain(null, scope, expression.getReferencedName());
                reference[0] = expression;
            }

            @Override
            public void visitExpression(JetExpression expression) {
                // <e> create a dummy domain for the type of e
                throw new UnsupportedOperationException(); // TODO
            }

            @Override
            public void visitJetElement(JetElement elem) {
                throw new IllegalArgumentException("Unsupported element: " + elem);
            }
        });
        return new OverloadDomain() {
            @Override
            public FunctionDescriptor getFunctionDescriptorForNamedArguments(@NotNull List<Type> typeArguments, @NotNull Map<String, Type> valueArgumentTypes, @Nullable Type functionLiteralArgumentType) {
                FunctionDescriptor descriptor = result[0].getFunctionDescriptorForNamedArguments(typeArguments, valueArgumentTypes, functionLiteralArgumentType);
                trace.recordResolutionResult(reference[0], descriptor);
                return descriptor;
            }

            @Override
            public FunctionDescriptor getFunctionDescriptorForPositionedArguments(@NotNull List<Type> typeArguments, @NotNull List<Type> positionedValueArgumentTypes) {
                FunctionDescriptor descriptor = result[0].getFunctionDescriptorForPositionedArguments(typeArguments, positionedValueArgumentTypes);
                trace.recordResolutionResult(reference[0], descriptor);
                return descriptor;
            }
        };
    }

    private Type getBlockReturnedType(@NotNull JetScope outerScope, List<JetElement> block) {
        if (block.isEmpty()) {
            return JetStandardClasses.getUnitType();
        } else {
            WritableScope scope = new WritableScope(outerScope);
            for (JetElement statement : block) {
                // TODO: consider other declarations
                if (statement instanceof JetProperty) {
                    JetProperty property = (JetProperty) statement;
                    scope.addPropertyDescriptor(semanticServices.getClassDescriptorResolver().resolvePropertyDescriptor(scope, property));
                }
            }
            JetElement lastElement = block.get(block.size() - 1);
            if (lastElement instanceof JetExpression) {
                JetExpression expression = (JetExpression) lastElement;
                return getType(scope, expression, true);
            }
            // TODO: functions, classes, etc.
            throw new IllegalArgumentException("Last item in the block must be an expression");
        }
    }

    private void collectAllReturnTypes(JetWhenExpression whenExpression, JetScope scope, List<Type> result) {
        for (JetWhenEntry entry : whenExpression.getEntries()) {
            JetWhenExpression subWhen = entry.getSubWhen();
            if (subWhen != null) {
                collectAllReturnTypes(subWhen, scope, result);
            } else {
                JetExpression resultExpression = entry.getExpression();
                if (resultExpression != null) {
                    result.add(getType(scope, resultExpression, true));
                }
            }
        }
    }

}
