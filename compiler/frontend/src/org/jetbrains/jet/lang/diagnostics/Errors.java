package org.jetbrains.jet.lang.diagnostics;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.resolve.DescriptorRenderer;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Iterator;

import static org.jetbrains.jet.lang.diagnostics.Severity.ERROR;
import static org.jetbrains.jet.lang.diagnostics.Severity.WARNING;

/**
 * @author abreslav
 */
public interface Errors {

    UnresolvedReferenceDiagnosticFactory UNRESOLVED_REFERENCE = UnresolvedReferenceDiagnosticFactory.INSTANCE;
    RedeclarationDiagnosticFactory REDECLARATION = RedeclarationDiagnosticFactory.INSTANCE;
    PsiElementOnlyDiagnosticFactory2<JetType, JetType> TYPE_MISMATCH = PsiElementOnlyDiagnosticFactory2.create(ERROR, "Type mismatch: inferred type is {1} but {0} was expected");

    SimpleDiagnosticFactory SAFE_CALLS_ARE_NOT_ALLOWED_ON_NAMESPACES = SimpleDiagnosticFactory.create(ERROR, "Safe calls are not allowed on namespaces");
    SimpleDiagnosticFactory TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM = SimpleDiagnosticFactory.create(ERROR, "Type checking has run into a recursive problem"); // TODO: message
    SimpleDiagnosticFactory RETURN_NOT_ALLOWED = SimpleDiagnosticFactory.create(ERROR, "'return' is not allowed here");
    SimpleDiagnosticFactory PROJECTION_IN_IMMEDIATE_ARGUMENT_TO_SUPERTYPE = SimpleDiagnosticFactory.create(ERROR, "Projections are not allowed for immediate arguments of a supertype");
    SimpleDiagnosticFactory LABEL_NAME_CLASH = SimpleDiagnosticFactory.create(WARNING, "There is more than one label with such a name in this scope");
    SimpleDiagnosticFactory EXPRESSION_EXPECTED_NAMESPACE_FOUND = SimpleDiagnosticFactory.create(ERROR, "Expression expected, but a namespace name found");
    SimpleDiagnosticFactory CANNOT_INFER_PARAMETER_TYPE = SimpleDiagnosticFactory.create(ERROR, "Cannot infer a type for this parameter. To specify it explicitly use the {(p : Type) => ...} notation");

    SimpleDiagnosticFactory NO_BACKING_FIELD = SimpleDiagnosticFactory.create(ERROR, "This property does not have a backing field");

    SimpleDiagnosticFactory MIXING_NAMED_AND_POSITIONED_ARGUMENTS = SimpleDiagnosticFactory.create(ERROR, "Mixing named and positioned arguments in not allowed");
    SimpleDiagnosticFactory ARGUMENT_PASSED_TWICE = SimpleDiagnosticFactory.create(ERROR, "An argument is already passed for this parameter");
    SimpleDiagnosticFactory NAMED_PARAMETER_NOT_FOUND = SimpleDiagnosticFactory.create(ERROR, "Cannot find a parameter with this name");
    SimpleDiagnosticFactory VARARG_OUTSIDE_PARENTHESES = SimpleDiagnosticFactory.create(ERROR, "Passing value as a vararg is only allowed inside a parenthesized argument list");
    SimpleDiagnosticFactory MANY_FUNCTION_LITERAL_ARGUMENTS = SimpleDiagnosticFactory.create(ERROR, "Only one function literal is allowed outside a parenthesized argument list");

    SimpleDiagnosticFactory PROPERTY_WITH_NO_TYPE_NO_INITIALIZER = SimpleDiagnosticFactory.create(ERROR, "This property must either have a type annotation or be initialized");
    SimpleDiagnosticFactory FUNCTION_WITH_NO_TYPE_NO_BODY = SimpleDiagnosticFactory.create(ERROR, "This function must either declare a return type or have a body element");

    SimpleDiagnosticFactory ABSTRACT_PROPERTY_IN_PRIMARY_CONSTRUCTOR_PARAMETERS = SimpleDiagnosticFactory.create(ERROR, "This property cannot be declared abstract");
    SimpleDiagnosticFactory ABSTRACT_PROPERTY_NOT_IN_CLASS = SimpleDiagnosticFactory.create(ERROR, "A property may be abstract only when defined in a class or trait");
    SimpleDiagnosticFactory ABSTRACT_PROPERTY_WITH_INITIALIZER = SimpleDiagnosticFactory.create(ERROR, "Property with initializer cannot be abstract");
    SimpleDiagnosticFactory ABSTRACT_PROPERTY_WITH_GETTER = SimpleDiagnosticFactory.create(ERROR, "Property with getter implementation cannot be abstract");
    SimpleDiagnosticFactory ABSTRACT_PROPERTY_WITH_SETTER = SimpleDiagnosticFactory.create(ERROR, "Property with setter implementation cannot be abstract");
    SimpleDiagnosticFactory BACKING_FIELD_IN_TRAIT = SimpleDiagnosticFactory.create(ERROR, "Property in a trait cannot have a backing field");
    SimpleDiagnosticFactory MUST_BE_INITIALIZED = SimpleDiagnosticFactory.create(ERROR, "Property must be initialized");
    SimpleDiagnosticFactory MUST_BE_INITIALIZED_OR_BE_ABSTRACT = SimpleDiagnosticFactory.create(ERROR, "Property must be initialized or be abstract");
    SimpleDiagnosticFactory PROPERTY_INITIALIZER_IN_TRAIT = SimpleDiagnosticFactory.create(ERROR, "Property initializers are not allowed in traits");
    SimpleDiagnosticFactory PROPERTY_INITIALIZER_NO_BACKING_FIELD = SimpleDiagnosticFactory.create(ERROR, "Initializer is not allowed here because this property has no backing field");
    SimpleDiagnosticFactory PROPERTY_INITIALIZER_NO_PRIMARY_CONSTRUCTOR = SimpleDiagnosticFactory.create(ERROR, "Property initializers are not allowed when no primary constructor is present");
    SimpleDiagnosticFactory REDUNDANT_ABSTRACT = SimpleDiagnosticFactory.create(WARNING, "Abstract modifier is redundant in traits");
    ParameterizedDiagnosticFactory2<String, ClassDescriptor> ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS = ParameterizedDiagnosticFactory2.create(ERROR, "Abstract property {0} in non-abstract class {1}");
    ParameterizedDiagnosticFactory2<String, ClassDescriptor> ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS = ParameterizedDiagnosticFactory2.create(ERROR, "Abstract function {0} in non-abstract class {1}");
    ParameterizedDiagnosticFactory1<FunctionDescriptor> ABSTRACT_FUNCTION_WITH_BODY = ParameterizedDiagnosticFactory1.create(ERROR, "A function {0} with body cannot be abstract");
    ParameterizedDiagnosticFactory1<FunctionDescriptor> NON_ABSTRACT_FUNCTION_WITH_NO_BODY = ParameterizedDiagnosticFactory1.create(ERROR, "Method {0} without a body must be abstract");
    ParameterizedDiagnosticFactory1<FunctionDescriptor> NON_MEMBER_ABSTRACT_FUNCTION = ParameterizedDiagnosticFactory1.create(ERROR, "Function {0} is not a class or trait member and cannot be abstract");
    SimpleDiagnosticFactory NON_MEMBER_ABSTRACT_ACCESSOR = SimpleDiagnosticFactory.create(ERROR, "This property is not a class or trait member and thus cannot have abstract accessors"); // TODO : Better message
    ParameterizedDiagnosticFactory1<FunctionDescriptor> NON_MEMBER_FUNCTION_NO_BODY = ParameterizedDiagnosticFactory1.create(ERROR, "Function {0} must have a body");

    SimpleDiagnosticFactory PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT = SimpleDiagnosticFactory.create(ERROR, "Projections are not allowed on type arguments of functions and properties"); // TODO : better positioning

    SimpleDiagnosticFactory SUPERTYPE_NOT_INITIALIZED = SimpleDiagnosticFactory.create(ERROR, "This type has a constructor, and thus must be initialized here");
    SimpleDiagnosticFactory SECONDARY_CONSTRUCTOR_BUT_NO_PRIMARY = SimpleDiagnosticFactory.create(ERROR, "A secondary constructor may appear only in a class that has a primary constructor");
    SimpleDiagnosticFactory SECONDARY_CONSTRUCTOR_NO_INITIALIZER_LIST = SimpleDiagnosticFactory.create(ERROR, "Secondary constructors must have an initializer list");
    SimpleDiagnosticFactory BY_IN_SECONDARY_CONSTRUCTOR = SimpleDiagnosticFactory.create(ERROR, "'by'-clause is only supported for primary constructors");
    SimpleDiagnosticFactory INITIALIZER_WITH_NO_ARGUMENTS = SimpleDiagnosticFactory.create(ERROR, "Constructor arguments required");
    SimpleDiagnosticFactory MANY_CALLS_TO_THIS = SimpleDiagnosticFactory.create(ERROR, "Only one call to 'this(...)' is allowed");
    ParameterizedDiagnosticFactory1<FunctionDescriptor> NOTHING_TO_OVERRIDE = ParameterizedDiagnosticFactory1.create(ERROR, "Function {0} overrides nothing");
    ParameterizedDiagnosticFactory1<PropertyDescriptor> PRIMARY_CONSTRUCTOR_MISSING_STATEFUL_PROPERTY = ParameterizedDiagnosticFactory1.create(ERROR, "This class must have a primary constructor, because property {0} has a backing field");
    ParameterizedDiagnosticFactory1<JetClassOrObject> PRIMARY_CONSTRUCTOR_MISSING_SUPER_CONSTRUCTOR_CALL = new ParameterizedDiagnosticFactory1<JetClassOrObject>(ERROR, "Class {0} must have a constructor in order to be able to initialize supertypes") {
        @Override
        protected String makeMessageFor(@NotNull JetClassOrObject argument) {
            return JetPsiUtil.safeName(argument.getName());
        }
    };
    ParameterizedDiagnosticFactory1<Throwable> EXCEPTION_WHILE_ANALYZING = new ParameterizedDiagnosticFactory1<Throwable>(ERROR, "{0}") {
        @Override
        protected String makeMessageFor(@NotNull Throwable e) {
            return e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    };
    ParameterizedDiagnosticFactory3<FunctionDescriptor, FunctionDescriptor, DeclarationDescriptor> VIRTUAL_METHOD_HIDDEN = ParameterizedDiagnosticFactory3.create(ERROR, "Function '{0}' hides '{1}' in class {2} and needs 'override' modifier");

    SimpleDiagnosticFactory UNREACHABLE_CODE = SimpleDiagnosticFactory.create(ERROR, "Unreachable code");
    ParameterizedDiagnosticFactory1<String> UNREACHABLE_BECAUSE_OF_NOTHING = ParameterizedDiagnosticFactory1.create(ERROR, "This code is unreachable, because '{0}' never terminates normally");

    SimpleDiagnosticFactory MANY_CLASS_OBJECTS = SimpleDiagnosticFactory.create(ERROR, "Only one class object is allowed per class");
    SimpleDiagnosticFactory CLASS_OBJECT_NOT_ALLOWED = SimpleDiagnosticFactory.create(ERROR, "A class object is not allowed here");
    SimpleDiagnosticFactory DELEGATION_IN_TRAIT = SimpleDiagnosticFactory.create(ERROR, "Traits cannot use delegation");
    SimpleDiagnosticFactory NO_CONSTRUCTOR = SimpleDiagnosticFactory.create(ERROR, "This class does not have a constructor");
    SimpleDiagnosticFactory NOT_A_CLASS = SimpleDiagnosticFactory.create(ERROR, "Not a class");
    SimpleDiagnosticFactory ILLEGAL_ESCAPE_SEQUENCE = SimpleDiagnosticFactory.create(ERROR, "Illegal escape sequence");

    SimpleDiagnosticFactory LOCAL_EXTENSION_PROPERTY = SimpleDiagnosticFactory.create(ERROR, "Local extension properties are not allowed");
    SimpleDiagnosticFactory LOCAL_VARIABLE_WITH_GETTER = SimpleDiagnosticFactory.create(ERROR, "Local variables are not allowed to have getters");
    SimpleDiagnosticFactory LOCAL_VARIABLE_WITH_SETTER = SimpleDiagnosticFactory.create(ERROR, "Local variables are not allowed to have setters");
    SimpleDiagnosticFactory VAL_WITH_SETTER = SimpleDiagnosticFactory.create(ERROR, "A 'val'-property cannot have a setter");

    SimpleDiagnosticFactory EQUALS_MISSING = SimpleDiagnosticFactory.create(ERROR, "No method 'equals(Any?) : Boolean' available");
    SimpleDiagnosticFactory ASSIGNMENT_IN_EXPRESSION_CONTEXT = SimpleDiagnosticFactory.create(ERROR, "Assignments are not expressions, and only expressions are allowed in this context");
    SimpleDiagnosticFactory NAMESPACE_IS_NOT_AN_EXPRESSION = SimpleDiagnosticFactory.create(ERROR, "'namespace' is not an expression");
    SimpleDiagnosticFactory DECLARATION_IN_ILLEGAL_CONTEXT = SimpleDiagnosticFactory.create(ERROR, "Declarations are not allowed in this position");
    SimpleDiagnosticFactory REF_SETTER_PARAMETER = SimpleDiagnosticFactory.create(ERROR, "Setter parameters can not be 'ref'");
    SimpleDiagnosticFactory SETTER_PARAMETER_WITH_DEFAULT_VALUE = SimpleDiagnosticFactory.create(ERROR, "Setter parameters can not have default values");
    SimpleDiagnosticFactory NO_THIS = SimpleDiagnosticFactory.create(ERROR, "'this' is not defined in this context");
    SimpleDiagnosticFactory NOT_A_SUPERTYPE = SimpleDiagnosticFactory.create(ERROR, "Not a supertype");
    SimpleDiagnosticFactory NO_WHEN_ENTRIES = SimpleDiagnosticFactory.create(ERROR, "Entries are required for when-expression"); // TODO : Scope, and maybe this should not be an error
    SimpleDiagnosticFactory USELESS_CAST_STATIC_ASSERT_IS_FINE = SimpleDiagnosticFactory.create(WARNING, "No cast needed, use ':' instead");
    SimpleDiagnosticFactory USELESS_CAST = SimpleDiagnosticFactory.create(WARNING, "No cast needed");
    SimpleDiagnosticFactory CAST_NEVER_SUCCEEDS = SimpleDiagnosticFactory.create(WARNING, "This cast can never succeed");
    ParameterizedDiagnosticFactory1<JetType> WRONG_SETTER_PARAMETER_TYPE = ParameterizedDiagnosticFactory1.create(ERROR, "Setter parameter type must be equal to the type of the property, i.e. {0}");
    ParameterizedDiagnosticFactory1<JetType> WRONG_GETTER_RETURN_TYPE = ParameterizedDiagnosticFactory1.create(ERROR, "Getter return type must be equal to the type of the property, i.e. {0}");
    ParameterizedDiagnosticFactory1<ClassifierDescriptor> NO_CLASS_OBJECT = new ParameterizedDiagnosticFactory1<ClassifierDescriptor>(ERROR, "Classifier {0} does not have a class object") {
        @Override
        protected String makeMessageFor(@NotNull ClassifierDescriptor argument) {
            return argument.getName();
        }
    };

    SimpleDiagnosticFactory NO_GENERICS_IN_SUPERTYPE_SPECIFIER = SimpleDiagnosticFactory.create(ERROR, "Generic arguments of the base type must be specified");

    SimpleDiagnosticFactory HAS_NEXT_PROPERTY_AND_FUNCTION_AMBIGUITY = SimpleDiagnosticFactory.create(ERROR, "An ambiguity between 'iterator().hasNext()' function and 'iterator().hasNext' property");
    SimpleDiagnosticFactory HAS_NEXT_MISSING = SimpleDiagnosticFactory.create(ERROR, "Loop range must have an 'iterator().hasNext()' function or an 'iterator().hasNext' property");
    SimpleDiagnosticFactory HAS_NEXT_FUNCTION_AMBIGUITY = SimpleDiagnosticFactory.create(ERROR, "Function 'iterator().hasNext()' is ambiguous for this expression");
    SimpleDiagnosticFactory HAS_NEXT_MUST_BE_READABLE = SimpleDiagnosticFactory.create(ERROR, "The 'iterator().hasNext' property of the loop range must be readable");
    ParameterizedDiagnosticFactory1<JetType> HAS_NEXT_PROPERTY_TYPE_MISMATCH = ParameterizedDiagnosticFactory1.create(ERROR, "The 'iterator().hasNext' property of the loop range must return Boolean, but returns {0}");
    ParameterizedDiagnosticFactory1<JetType> HAS_NEXT_FUNCTION_TYPE_MISMATCH = ParameterizedDiagnosticFactory1.create(ERROR, "The 'iterator().hasNext()' function of the loop range must return Boolean, but returns {0}");
    SimpleDiagnosticFactory NEXT_AMBIGUITY = SimpleDiagnosticFactory.create(ERROR, "Function 'iterator().next()' is ambiguous for this expression");
    SimpleDiagnosticFactory NEXT_MISSING = SimpleDiagnosticFactory.create(ERROR, "Loop range must have an 'iterator().next()' function");
    SimpleDiagnosticFactory ITERATOR_MISSING = SimpleDiagnosticFactory.create(ERROR, "For-loop range must have an iterator() method");
    AmbiguousDescriptorDiagnosticFactory ITERATOR_AMBIGUITY = AmbiguousDescriptorDiagnosticFactory.create("Method 'iterator()' is ambiguous for this expression: {0}");

    ParameterizedDiagnosticFactory1<JetType> COMPARE_TO_TYPE_MISMATCH = ParameterizedDiagnosticFactory1.create(ERROR, "compareTo() must return Int, but returns {0}");

    SimpleDiagnosticFactory RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY = SimpleDiagnosticFactory.create(ERROR, "Returns are not allowed for functions with expression body. Use block body in '{...}'");
    SimpleDiagnosticFactory NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY = SimpleDiagnosticFactory.create(ERROR, "A 'return' expression required in a function with a block body ('{...}')");
    ParameterizedDiagnosticFactory1<JetType> RETURN_TYPE_MISMATCH = ParameterizedDiagnosticFactory1.create(ERROR, "This function must return a value of type {0}");

    ParameterizedDiagnosticFactory1<JetType> UPPER_BOUND_VIOLATED = ParameterizedDiagnosticFactory1.create(ERROR, "An upper bound {0} is violated"); // TODO : Message
    ParameterizedDiagnosticFactory1<JetType> FINAL_CLASS_OBJECT_UPPER_BOUND = ParameterizedDiagnosticFactory1.create(ERROR, "{0} is a final type, and thus a class object cannot extend it");
    ParameterizedDiagnosticFactory1<JetType> FINAL_UPPER_BOUND = ParameterizedDiagnosticFactory1.create(WARNING, "{0} is a final type, and thus a value of the type parameter is predetermined");
    ParameterizedDiagnosticFactory1<JetType> USELESS_ELVIS = ParameterizedDiagnosticFactory1.create(WARNING, "Elvis operator (?:) always returns the left operand of non-nullable type {0}");
    ParameterizedDiagnosticFactory1<TypeParameterDescriptor> CONFLICTING_UPPER_BOUNDS = new ParameterizedDiagnosticFactory1<TypeParameterDescriptor>(ERROR, "Upper bounds of {0} have empty intersection") {
        @Override
        protected String makeMessageFor(@NotNull TypeParameterDescriptor argument) {
            return argument.getName();
        }
    };
    ParameterizedDiagnosticFactory1<TypeParameterDescriptor> CONFLICTING_CLASS_OBJECT_UPPER_BOUNDS = new ParameterizedDiagnosticFactory1<TypeParameterDescriptor>(ERROR, "Class object upper bounds of {0} have empty intersection") {
        @Override
        protected String makeMessageFor(@NotNull TypeParameterDescriptor argument) {
            return argument.getName();
        }
    };

    ParameterizedDiagnosticFactory1<CallableDescriptor> TOO_MANY_ARGUMENTS = ParameterizedDiagnosticFactory1.create(ERROR, "Too many arguments for {0}");
    ParameterizedDiagnosticFactory1<String> ERROR_COMPILE_TIME_VALUE = ParameterizedDiagnosticFactory1.create(ERROR, "{0}");

    SimpleDiagnosticFactory ELSE_MISPLACED_IN_WHEN = SimpleDiagnosticFactory.create(ERROR, "'else' entry must be the last one in a when-expression");
    SimpleDiagnosticFactory CYCLIC_INHERITANCE_HIERARCHY = SimpleDiagnosticFactory.create(ERROR, "There's a cycle in the inheritance hierarchy for this type");

    SimpleDiagnosticFactory MANY_CLASSES_IN_SUPERTYPE_LIST = SimpleDiagnosticFactory.create(ERROR, "Only one class may appear in a supertype list");
    SimpleDiagnosticFactory SUPERTYPE_NOT_A_CLASS_OR_TRAIT = SimpleDiagnosticFactory.create(ERROR, "Only classes and traits may serve as supertypes");
    SimpleDiagnosticFactory SUPERTYPE_INITIALIZED_IN_TRAIT = SimpleDiagnosticFactory.create(ERROR, "Traits cannot initialize supertypes");
    SimpleDiagnosticFactory CONSTRUCTOR_IN_TRAIT = SimpleDiagnosticFactory.create(ERROR, "A trait may not have a constructor");
    SimpleDiagnosticFactory SUPERTYPE_APPEARS_TWICE = SimpleDiagnosticFactory.create(ERROR, "A supertype appears twice");
    SimpleDiagnosticFactory FINAL_SUPERTYPE = SimpleDiagnosticFactory.create(ERROR, "This type is final, so it cannot be inherited from");

    SimpleDiagnosticFactory REF_PARAMETER_WITH_VAL_OR_VAR = SimpleDiagnosticFactory.create(ERROR, "'val' and 'var' are not allowed on ref-parameters");
    SimpleDiagnosticFactory VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION = SimpleDiagnosticFactory.create(ERROR, "A type annotation is required on a value parameter");
    SimpleDiagnosticFactory BREAK_OR_CONTINUE_OUTSIDE_A_LOOP = SimpleDiagnosticFactory.create(ERROR, "'break' and 'continue' are only allowed inside a loop");
    ParameterizedDiagnosticFactory1<String> NOT_A_LOOP_LABEL = ParameterizedDiagnosticFactory1.create(ERROR, "The label '{0}' does not denote a loop");

    SimpleDiagnosticFactory ANONYMOUS_INITIALIZER_WITHOUT_CONSTRUCTOR = SimpleDiagnosticFactory.create(ERROR, "Anonymous initializers are only allowed in the presence of a primary constructor");
    SimpleDiagnosticFactory NULLABLE_SUPERTYPE = SimpleDiagnosticFactory.create(ERROR, "A supertype cannot be nullable");
    ParameterizedDiagnosticFactory1<JetType> UNSAFE_CALL = ParameterizedDiagnosticFactory1.create(ERROR, "Only safe calls (?.) are allowed on a nullable receiver of type {0}");
    SimpleDiagnosticFactory AMBIGUOUS_LABEL = SimpleDiagnosticFactory.create(ERROR, "Ambiguous label");
    ParameterizedDiagnosticFactory1<String> UNSUPPORTED = ParameterizedDiagnosticFactory1.create(ERROR, "Unsupported [{0}]");
    ParameterizedDiagnosticFactory1<JetType> UNNECESSARY_SAFE_CALL = ParameterizedDiagnosticFactory1.create(WARNING, "Unnecessary safe call on a non-null receiver of type {0}");
    ParameterizedDiagnosticFactory2<JetTypeConstraint, JetTypeParameterListOwner> NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER = new ParameterizedDiagnosticFactory2<JetTypeConstraint, JetTypeParameterListOwner>(ERROR, "{0} does not refer to a type parameter of {1}") {
        @Override
        protected String makeMessageForA(@NotNull JetTypeConstraint jetTypeConstraint) {
            return jetTypeConstraint.getSubjectTypeParameterName().getReferencedName();
        }

        @Override
        protected String makeMessageForB(@NotNull JetTypeParameterListOwner constraintOwner) {
            return constraintOwner.getName();
        }
    };
    ParameterizedDiagnosticFactory2<JetType, VariableDescriptor> AUTOCAST_IMPOSSIBLE = new ParameterizedDiagnosticFactory2<JetType, VariableDescriptor>(ERROR, "Automatic cast to {0} is impossible, because variable {1} is mutable") {
        @Override
        protected String makeMessageForB(@NotNull VariableDescriptor variableDescriptor) {
            return variableDescriptor.getName();
        }
    };

    ParameterizedDiagnosticFactory2<JetType, JetType> TYPE_MISMATCH_IN_FOR_LOOP = ParameterizedDiagnosticFactory2.create(ERROR, "The loop iterates over values of type {0} but the parameter is declared to be {1}");
    ParameterizedDiagnosticFactory1<JetType> TYPE_MISMATCH_IN_CONDITION = ParameterizedDiagnosticFactory1.create(ERROR, "Condition must be of type Boolean, but was of type {0}");
    ParameterizedDiagnosticFactory2<JetType, Integer> TYPE_MISMATCH_IN_TUPLE_PATTERN = ParameterizedDiagnosticFactory2.create(ERROR, "Type mismatch: subject is of type {0} but the pattern is of type Tuple{1}"); // TODO: message
    ParameterizedDiagnosticFactory2<JetType, JetType> TYPE_MISMATCH_IN_BINDING_PATTERN = ParameterizedDiagnosticFactory2.create(ERROR, "{0} must be a supertype of {1}. Use 'is' to match against {0}");
    ParameterizedDiagnosticFactory2<JetType, JetType> INCOMPATIBLE_TYPES = ParameterizedDiagnosticFactory2.create(ERROR, "Incompatible types: {0} and {1}");
    
    ParameterizedDiagnosticFactory3<TypeParameterDescriptor, ClassDescriptor, Collection<JetType>> INCONSISTENT_TYPE_PARAMETER_VALUES = new ParameterizedDiagnosticFactory3<TypeParameterDescriptor, ClassDescriptor, Collection<JetType>>(ERROR, "Type parameter {0} of {1} has inconsistent values: {2}") {
        @Override
        protected String makeMessageForA(@NotNull TypeParameterDescriptor typeParameterDescriptor) {
            return typeParameterDescriptor.getName();
        }

        @Override
        protected String makeMessageForB(@NotNull ClassDescriptor classDescriptor) {
            return DescriptorRenderer.TEXT.render(classDescriptor);
        }

        @Override
        protected String makeMessageForC(@NotNull Collection<JetType> jetTypes) {
            StringBuilder builder = new StringBuilder();
            for (Iterator<JetType> iterator = jetTypes.iterator(); iterator.hasNext(); ) {
                JetType jetType = iterator.next();
                builder.append(jetType);
                if (iterator.hasNext()) {
                    builder.append(", ");
                }
            }
            return builder.toString();
        }
    };
    
    ParameterizedDiagnosticFactory3<JetSimpleNameExpression, JetType, JetType> EQUALITY_NOT_APPLICABLE = new ParameterizedDiagnosticFactory3<JetSimpleNameExpression, JetType, JetType>(ERROR, "Operator {0} cannot be applied to {1} and {2}") {
        @Override
        protected String makeMessageForA(@NotNull JetSimpleNameExpression nameExpression) {
            return nameExpression.getReferencedName();
        }
    };
    ParameterizedDiagnosticFactory2<FunctionDescriptor, DeclarationDescriptor> OVERRIDING_FINAL_FUNCTION = new ParameterizedDiagnosticFactory2<FunctionDescriptor, DeclarationDescriptor>(ERROR, "Method {0} in {1} is final and cannot be overridden") {
        @Override
        protected String makeMessageForA(@NotNull FunctionDescriptor functionDescriptor) {
            return functionDescriptor.getName();
        }

        @Override
        protected String makeMessageForB(@NotNull DeclarationDescriptor descriptor) {
            return descriptor.getName();
        }
    };
    ParameterizedDiagnosticFactory3<JetClassOrObject, FunctionDescriptor, DeclarationDescriptor> ABSTRACT_METHOD_NOT_IMPLEMENTED = new ParameterizedDiagnosticFactory3<JetClassOrObject, FunctionDescriptor, DeclarationDescriptor>(ERROR, "Class '{0}' must be declared abstract or implement abstract method '{1}' declared in {2}") {
        @Override
        protected String makeMessageForA(@NotNull JetClassOrObject jetClassOrObject) {
            return jetClassOrObject.getName();
        }

        @Override
        protected String makeMessageForB(@NotNull FunctionDescriptor functionDescriptor) {
            return DescriptorRenderer.TEXT.render(functionDescriptor);
        }

        @Override
        protected String makeMessageForC(@NotNull DeclarationDescriptor descriptor) {
            return descriptor.getName();
        }
    };

    ParameterizedDiagnosticFactory3<String, JetType, JetType> RESULT_TYPE_MISMATCH = ParameterizedDiagnosticFactory3.create(ERROR, "{0} must return {1} but returns {2}");
    ParameterizedDiagnosticFactory3<String, String, String> UNSAFE_INFIX_CALL = ParameterizedDiagnosticFactory3.create(ERROR, "Infix call corresponds to a dot-qualified call '{0}.{1}({2})' which is not allowed on a nullable receiver '{0}'. Use '?.'-qualified call instead");

    ParameterizedDiagnosticFactory1<Collection<? extends CallableDescriptor>> OVERLOAD_RESOLUTION_AMBIGUITY = new AmbiguousDescriptorDiagnosticFactory("Overload resolution ambiguity: {0}");
    ParameterizedDiagnosticFactory1<Collection<? extends CallableDescriptor>> NONE_APPLICABLE = new AmbiguousDescriptorDiagnosticFactory("None of the following functions can be called with the arguments supplied: {0}");
    ParameterizedDiagnosticFactory1<ValueParameterDescriptor> NO_VALUE_FOR_PARAMETER = ParameterizedDiagnosticFactory1.create(ERROR, "No value passed for parameter {0}");
    ParameterizedDiagnosticFactory1<JetType> MISSING_RECEIVER = ParameterizedDiagnosticFactory1.create(ERROR, "A receiver of type {0} is required");
    SimpleDiagnosticFactory NO_RECEIVER_ADMITTED = SimpleDiagnosticFactory.create(ERROR, "No receiver can be passed to this function or property");

    SimpleDiagnosticFactory CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS = SimpleDiagnosticFactory.create(ERROR, "Can not create an instance of an abstract class");
    SimpleDiagnosticFactory TYPE_INFERENCE_FAILED = SimpleDiagnosticFactory.create(ERROR, "Type inference failed");
    ParameterizedDiagnosticFactory1<Integer> WRONG_NUMBER_OF_TYPE_ARGUMENTS = new ParameterizedDiagnosticFactory1<Integer>(ERROR, "{0} type arguments expected") {
        @Override
        protected String makeMessageFor(@NotNull Integer argument) {
            return argument == 0 ? "No" : argument.toString();
        }
    };


    // This field is needed to make the Initializer class load (interfaces cannot have static initializers)
    @SuppressWarnings("UnusedDeclaration")
    Initializer __initializer = Initializer.INSTANCE;
    class Initializer {
        static {
            for (Field field : Errors.class.getFields()) {
                if ((field.getModifiers() & Modifier.STATIC) != 0) {
                    try {
                        Object value = field.get(null);
                        if (value instanceof AbstractDiagnosticFactory) {
                            AbstractDiagnosticFactory factory = (AbstractDiagnosticFactory) value;
                            factory.setName(field.getName());
                        }
                    }
                    catch (IllegalAccessException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
        }
        private static final Initializer INSTANCE = new Initializer();
        private Initializer() {};
    }
}
