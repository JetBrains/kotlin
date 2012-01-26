package org.jetbrains.jet.lang.diagnostics;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.calls.inference.SolutionStatus;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lexer.JetKeywordToken;
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

    Renderer NAME = new Renderer() {
        @NotNull
        @Override
        public String render(@Nullable Object object) {
            if (object == null) return "null";
            if (object instanceof Named) {
                return ((Named) object).getName();
            }
            return object.toString();
        }
    };

    ParameterizedDiagnosticFactory1<Throwable> EXCEPTION_WHILE_ANALYZING = new ParameterizedDiagnosticFactory1<Throwable>(ERROR, "{0}") {
        @Override
        protected String makeMessageFor(@NotNull Throwable e) {
            return e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    };

    UnresolvedReferenceDiagnosticFactory UNRESOLVED_REFERENCE = new UnresolvedReferenceDiagnosticFactory("Unresolved reference");

    RedeclarationDiagnosticFactory REDECLARATION = RedeclarationDiagnosticFactory.REDECLARATION;
    RedeclarationDiagnosticFactory NAME_SHADOWING = RedeclarationDiagnosticFactory.NAME_SHADOWING;

    PsiElementOnlyDiagnosticFactory2<PsiElement, JetType, JetType> TYPE_MISMATCH = PsiElementOnlyDiagnosticFactory2.create(ERROR, "Type mismatch: inferred type is {1} but {0} was expected");
    ParameterizedDiagnosticFactory1<Collection<JetKeywordToken>> INCOMPATIBLE_MODIFIERS = new ParameterizedDiagnosticFactory1<Collection<JetKeywordToken>>(ERROR, "Incompatible modifiers: ''{0}''") {
        @Override
        protected String makeMessageFor(Collection<JetKeywordToken> argument) {
            StringBuilder sb = new StringBuilder();
            for (Iterator<JetKeywordToken> iterator = argument.iterator(); iterator.hasNext(); ) {
                JetKeywordToken modifier =  iterator.next();
                sb.append(modifier.getValue());
                if (iterator.hasNext()) {
                    sb.append(" ");
                }
            }
            return sb.toString();
        }
    };
    DiagnosticWithParameterFactory<JetModifierList, JetKeywordToken> ILLEGAL_MODIFIER = DiagnosticWithParameterFactory.create(ERROR, "Illegal modifier ''{0}''", DiagnosticParameters.MODIFIER);

    PsiElementOnlyDiagnosticFactory2<JetModifierList, JetKeywordToken, JetKeywordToken> REDUNDANT_MODIFIER = new PsiElementOnlyDiagnosticFactory2<JetModifierList, JetKeywordToken, JetKeywordToken>(Severity.WARNING, "Modifier {0} is redundant because {1} is present") {
        @NotNull
        @Override
        public DiagnosticWithPsiElement<JetModifierList> on(@NotNull JetModifierList elementToBlame, @NotNull ASTNode nodeToMark, @NotNull JetKeywordToken redundantModifier, @NotNull JetKeywordToken presentModifier) {
            return super.on(elementToBlame, nodeToMark, redundantModifier, presentModifier).add(DiagnosticParameters.MODIFIER, redundantModifier);
        }
    };
    DiagnosticWithParameterFactory<JetModifierList, JetKeywordToken> REDUNDANT_MODIFIER_IN_TRAIT = DiagnosticWithParameterFactory.create(WARNING, "Modifier ''{0}'' is redundant in trait", DiagnosticParameters.MODIFIER);
    DiagnosticWithParameterFactory<JetModifierList, JetKeywordToken> REDUNDANT_MODIFIER_IN_GETTER = DiagnosticWithParameterFactory.create(WARNING, "Visibility modifiers are redundant in getter", DiagnosticParameters.MODIFIER);
    SimplePsiElementOnlyDiagnosticFactory<JetClass> TRAIT_CAN_NOT_BE_FINAL = SimplePsiElementOnlyDiagnosticFactory.create(ERROR, "Trait can not be final");
    SimpleDiagnosticFactory SAFE_CALLS_ARE_NOT_ALLOWED_ON_NAMESPACES = SimpleDiagnosticFactory.create(ERROR, "Safe calls are not allowed on namespaces");
    SimpleDiagnosticFactory TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM = SimpleDiagnosticFactory.create(ERROR, "Type checking has run into a recursive problem. Easiest workaround: specify types of your declarations explicitly"); // TODO: message
    SimpleDiagnosticFactory RETURN_NOT_ALLOWED = SimpleDiagnosticFactory.create(ERROR, "'return' is not allowed here");
    SimpleDiagnosticFactory PROJECTION_IN_IMMEDIATE_ARGUMENT_TO_SUPERTYPE = SimpleDiagnosticFactory.create(ERROR, "Projections are not allowed for immediate arguments of a supertype");
    SimpleDiagnosticFactory LABEL_NAME_CLASH = SimpleDiagnosticFactory.create(WARNING, "There is more than one label with such a name in this scope");
    SimpleDiagnosticFactory EXPRESSION_EXPECTED_NAMESPACE_FOUND = SimpleDiagnosticFactory.create(ERROR, "Expression expected, but a namespace name found");

    ParameterizedDiagnosticFactory1<DeclarationDescriptor> CANNOT_IMPORT_FROM_ELEMENT = ParameterizedDiagnosticFactory1.create(ERROR, "Cannot import from ''{0}''", NAME);
    ParameterizedDiagnosticFactory1<DeclarationDescriptor> CANNOT_BE_IMPORTED = ParameterizedDiagnosticFactory1.create(ERROR, "Cannot import ''{0}'', functions and properties can be imported only from packages", NAME);
    SimpleDiagnosticFactory USELESS_HIDDEN_IMPORT = SimpleDiagnosticFactory.create(WARNING, "Useless import, it is hidden further");
    SimpleDiagnosticFactory USELESS_SIMPLE_IMPORT = SimpleDiagnosticFactory.create(WARNING, "Useless import, does nothing");

    SimpleDiagnosticFactory CANNOT_INFER_PARAMETER_TYPE = SimpleDiagnosticFactory.create(ERROR, "Cannot infer a type for this parameter. To specify it explicitly use the {(p : Type) => ...} notation");

    SimpleDiagnosticFactory NO_BACKING_FIELD_ABSTRACT_PROPERTY = SimpleDiagnosticFactory.create(ERROR, "This property doesn't have a backing field, because it's abstract");
    SimpleDiagnosticFactory NO_BACKING_FIELD_CUSTOM_ACCESSORS = SimpleDiagnosticFactory.create(ERROR, "This property doesn't have a backing field, because it has custom accessors without reference to the backing field");
    SimpleDiagnosticFactory INACCESSIBLE_BACKING_FIELD = SimpleDiagnosticFactory.create(ERROR, "The backing field is not accessible here");
    SimpleDiagnosticFactory NOT_PROPERTY_BACKING_FIELD = SimpleDiagnosticFactory.create(ERROR, "The referenced variable is not a property and doesn't have backing field");

    SimpleDiagnosticFactory MIXING_NAMED_AND_POSITIONED_ARGUMENTS = SimpleDiagnosticFactory.create(ERROR, "Mixing named and positioned arguments in not allowed");
    SimpleDiagnosticFactory ARGUMENT_PASSED_TWICE = SimpleDiagnosticFactory.create(ERROR, "An argument is already passed for this parameter");
    UnresolvedReferenceDiagnosticFactory NAMED_PARAMETER_NOT_FOUND = new UnresolvedReferenceDiagnosticFactory("Cannot find a parameter with this name");//SimpleDiagnosticFactory.create(ERROR, "Cannot find a parameter with this name");
    SimpleDiagnosticFactory VARARG_OUTSIDE_PARENTHESES = SimpleDiagnosticFactory.create(ERROR, "Passing value as a vararg is only allowed inside a parenthesized argument list");

    SimpleDiagnosticFactory MANY_FUNCTION_LITERAL_ARGUMENTS = SimpleDiagnosticFactory.create(ERROR, "Only one function literal is allowed outside a parenthesized argument list");
    SimpleDiagnosticFactory PROPERTY_WITH_NO_TYPE_NO_INITIALIZER = SimpleDiagnosticFactory.create(ERROR, "This property must either have a type annotation or be initialized");

    SimpleDiagnosticFactory FUNCTION_WITH_NO_TYPE_NO_BODY = SimpleDiagnosticFactory.create(ERROR, "This function must either declare a return type or have a body element");
    SimplePsiElementOnlyDiagnosticFactory<JetModifierListOwner> ABSTRACT_PROPERTY_IN_PRIMARY_CONSTRUCTOR_PARAMETERS = SimplePsiElementOnlyDiagnosticFactory.create(ERROR, "This property cannot be declared abstract");
    SimplePsiElementOnlyDiagnosticFactory<JetModifierListOwner> ABSTRACT_PROPERTY_NOT_IN_CLASS = SimplePsiElementOnlyDiagnosticFactory.create(ERROR, "A property may be abstract only when defined in a class or trait");
    DiagnosticWithParameterFactory<JetProperty, JetType> ABSTRACT_PROPERTY_WITH_INITIALIZER = DiagnosticWithParameterFactory.create(ERROR, "Property with initializer cannot be abstract", DiagnosticParameters.TYPE);
    DiagnosticWithParameterFactory<JetProperty, JetType> ABSTRACT_PROPERTY_WITH_GETTER = DiagnosticWithParameterFactory.create(ERROR, "Property with getter implementation cannot be abstract", DiagnosticParameters.TYPE);
    DiagnosticWithParameterFactory<JetProperty, JetType> ABSTRACT_PROPERTY_WITH_SETTER = DiagnosticWithParameterFactory.create(ERROR, "Property with setter implementation cannot be abstract", DiagnosticParameters.TYPE);

    DiagnosticWithParameterFactory<JetModifierList, JetKeywordToken> GETTER_VISIBILITY_DIFFERS_FROM_PROPERTY_VISIBILITY = DiagnosticWithParameterFactory.create(ERROR, "Getter visibility must be the same as property visibility", DiagnosticParameters.MODIFIER);
    SimpleDiagnosticFactory BACKING_FIELD_IN_TRAIT = SimpleDiagnosticFactory.create(ERROR, "Property in a trait cannot have a backing field");
    SimpleDiagnosticFactory MUST_BE_INITIALIZED = SimpleDiagnosticFactory.create(ERROR, "Property must be initialized");
    SimplePsiElementOnlyDiagnosticFactory<JetModifierListOwner> MUST_BE_INITIALIZED_OR_BE_ABSTRACT = SimplePsiElementOnlyDiagnosticFactory.create(ERROR, "Property must be initialized or be abstract");
    DiagnosticWithParameterFactory<JetProperty, JetType> PROPERTY_INITIALIZER_IN_TRAIT = DiagnosticWithParameterFactory.create(ERROR, "Property initializers are not allowed in traits", DiagnosticParameters.TYPE);
    SimpleDiagnosticFactory PROPERTY_INITIALIZER_NO_BACKING_FIELD = SimpleDiagnosticFactory.create(ERROR, "Initializer is not allowed here because this property has no backing field");
    PsiElementOnlyDiagnosticFactory3<JetModifierListOwner, String, ClassDescriptor, JetClass> ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS = new PsiElementOnlyDiagnosticFactory3<JetModifierListOwner, String, ClassDescriptor, JetClass>(ERROR, "Abstract property {0} in non-abstract class {1}") {
        @NotNull
        protected DiagnosticWithPsiElement<JetModifierListOwner> on(@NotNull JetModifierListOwner elementToBlame, @NotNull TextRange textRangeToMark, @NotNull String s, @NotNull ClassDescriptor classDescriptor, @NotNull JetClass aClass) {
            return super.on(elementToBlame, textRangeToMark, s, classDescriptor, aClass).add(DiagnosticParameters.CLASS, aClass);
        }
    };
    PsiElementOnlyDiagnosticFactory3<JetFunction, String, ClassDescriptor, JetClass> ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS = new PsiElementOnlyDiagnosticFactory3<JetFunction, String, ClassDescriptor, JetClass>(ERROR, "Abstract function {0} in non-abstract class {1}") {
        @NotNull
        public DiagnosticWithPsiElement<JetFunction> on(@NotNull JetFunction elementToBlame, @NotNull ASTNode nodeToMark, @NotNull String s, @NotNull ClassDescriptor classDescriptor, @NotNull JetClass modifierListOwner) {
            return super.on(elementToBlame, nodeToMark, s, classDescriptor, modifierListOwner).add(DiagnosticParameters.CLASS, modifierListOwner);
        }
    };
    PsiElementOnlyDiagnosticFactory1<JetFunction, NamedFunctionDescriptor> ABSTRACT_FUNCTION_WITH_BODY = PsiElementOnlyDiagnosticFactory1.create(ERROR, "A function {0} with body cannot be abstract");
    PsiElementOnlyDiagnosticFactory1<JetFunction, NamedFunctionDescriptor> NON_ABSTRACT_FUNCTION_WITH_NO_BODY = PsiElementOnlyDiagnosticFactory1.create(ERROR, "Method {0} without a body must be abstract");
    PsiElementOnlyDiagnosticFactory1<JetModifierListOwner, NamedFunctionDescriptor> NON_MEMBER_ABSTRACT_FUNCTION = PsiElementOnlyDiagnosticFactory1.create(ERROR, "Function {0} is not a class or trait member and cannot be abstract");

    PsiElementOnlyDiagnosticFactory1<JetFunction, NamedFunctionDescriptor> NON_MEMBER_FUNCTION_NO_BODY = PsiElementOnlyDiagnosticFactory1.create(ERROR, "Function {0} must have a body");
    
    DiagnosticWithParameterFactory<JetNamedDeclaration, JetClass> NON_FINAL_MEMBER_IN_FINAL_CLASS = DiagnosticWithParameterFactory.create(ERROR, "Non final member in a final class", DiagnosticParameters.CLASS);

    DiagnosticWithParameterFactory<JetNamedDeclaration, JetType> PUBLIC_MEMBER_SHOULD_SPECIFY_TYPE = DiagnosticWithParameterFactory.create(ERROR, "Public or protected member should specify a type", DiagnosticParameters.TYPE);

    SimpleDiagnosticFactory PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT = SimpleDiagnosticFactory.create(ERROR, "Projections are not allowed on type arguments of functions and properties"); // TODO : better positioning
    SimpleDiagnosticFactory SUPERTYPE_NOT_INITIALIZED = SimpleDiagnosticFactory.create(ERROR, "This type has a constructor, and thus must be initialized here");
    SimplePsiElementOnlyDiagnosticFactory<JetDelegatorToSuperClass> SUPERTYPE_NOT_INITIALIZED_DEFAULT = SimplePsiElementOnlyDiagnosticFactory.create(ERROR, "Constructor invocation should be explicitly specified");
    SimpleDiagnosticFactory SECONDARY_CONSTRUCTOR_BUT_NO_PRIMARY = SimpleDiagnosticFactory.create(ERROR, "A secondary constructor may appear only in a class that has a primary constructor");
    SimpleDiagnosticFactory SECONDARY_CONSTRUCTOR_NO_INITIALIZER_LIST = SimpleDiagnosticFactory.create(ERROR, "Secondary constructors must have an initializer list");
    SimpleDiagnosticFactory BY_IN_SECONDARY_CONSTRUCTOR = SimpleDiagnosticFactory.create(ERROR, "'by'-clause is only supported for primary constructors");
    SimpleDiagnosticFactory INITIALIZER_WITH_NO_ARGUMENTS = SimpleDiagnosticFactory.create(ERROR, "Constructor arguments required");
    SimpleDiagnosticFactory MANY_CALLS_TO_THIS = SimpleDiagnosticFactory.create(ERROR, "Only one call to 'this(...)' is allowed");
    PsiElementOnlyDiagnosticFactory1<JetModifierList, CallableMemberDescriptor> NOTHING_TO_OVERRIDE = PsiElementOnlyDiagnosticFactory1.create(ERROR, "{0} overrides nothing", DescriptorRenderer.TEXT);
    PsiElementOnlyDiagnosticFactory3<JetModifierListOwner, CallableMemberDescriptor, CallableMemberDescriptor, DeclarationDescriptor> VIRTUAL_MEMBER_HIDDEN = PsiElementOnlyDiagnosticFactory3.create(ERROR, "''{0}'' hides ''{1}'' in class {2} and needs 'override' modifier", DescriptorRenderer.TEXT);

    PsiElementOnlyDiagnosticFactory1<JetSimpleNameExpression, VariableDescriptor> UNINITIALIZED_VARIABLE = PsiElementOnlyDiagnosticFactory1.create(ERROR, "Variable ''{0}'' must be initialized", NAME);
    PsiElementOnlyDiagnosticFactory1<JetSimpleNameExpression, ValueParameterDescriptor> UNINITIALIZED_PARAMETER = PsiElementOnlyDiagnosticFactory1.create(ERROR, "Parameter ''{0}'' is uninitialized here", NAME);
    UnusedElementDiagnosticFactory<JetProperty, VariableDescriptor> UNUSED_VARIABLE = UnusedElementDiagnosticFactory.create(WARNING, "Variable ''{0}'' is never used", NAME);
    UnusedElementDiagnosticFactory<JetParameter, VariableDescriptor> UNUSED_PARAMETER = UnusedElementDiagnosticFactory.create(WARNING, "Parameter ''{0}'' is never used", NAME);
    UnusedElementDiagnosticFactory<JetNamedDeclaration, DeclarationDescriptor> ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE = UnusedElementDiagnosticFactory.create(WARNING, "Variable ''{0}'' is assigned but never accessed", NAME);
    PsiElementOnlyDiagnosticFactory1<JetNamedDeclaration, DeclarationDescriptor> VARIABLE_WITH_REDUNDANT_INITIALIZER = PsiElementOnlyDiagnosticFactory1.create(WARNING, "Variable ''{0}'' initializer is redundant", NAME);
    PsiElementOnlyDiagnosticFactory2<JetElement, JetElement, DeclarationDescriptor> UNUSED_VALUE = new PsiElementOnlyDiagnosticFactory2<JetElement, JetElement, DeclarationDescriptor>(WARNING, "The value ''{0}'' assigned to ''{1}'' is never used", NAME) {
        @Override
        protected String makeMessageForA(@NotNull JetElement element) {
            return element.getText();
        }
    };
    PsiElementOnlyDiagnosticFactory1<JetElement, JetElement> UNUSED_CHANGED_VALUE = new PsiElementOnlyDiagnosticFactory1<JetElement, JetElement>(WARNING, "The value changed at ''{0}'' is never used", NAME) {
        @Override
        protected String makeMessageFor(JetElement argument) {
            return argument.getText();
        }
    };

    PsiElementOnlyDiagnosticFactory1<JetExpression, DeclarationDescriptor> VAL_REASSIGNMENT = PsiElementOnlyDiagnosticFactory1.create(ERROR, "Val can not be reassigned", NAME);
    PsiElementOnlyDiagnosticFactory1<JetExpression, DeclarationDescriptor> INITIALIZATION_BEFORE_DECLARATION = PsiElementOnlyDiagnosticFactory1.create(ERROR, "Variable cannot be initialized before declaration", NAME);
    SimplePsiElementOnlyDiagnosticFactory<JetExpression> VARIABLE_EXPECTED = new SimplePsiElementOnlyDiagnosticFactory<JetExpression>(ERROR, "Variable expected");

    PsiElementOnlyDiagnosticFactory1<JetSimpleNameExpression, DeclarationDescriptor> INITIALIZATION_USING_BACKING_FIELD_CUSTOM_SETTER = PsiElementOnlyDiagnosticFactory1.create(ERROR, "This property has a custom setter, so initialization using backing field required", NAME);
    PsiElementOnlyDiagnosticFactory1<JetSimpleNameExpression, DeclarationDescriptor> INITIALIZATION_USING_BACKING_FIELD_OPEN_SETTER = PsiElementOnlyDiagnosticFactory1.create(ERROR, "Setter of this property can be overridden, so initialization using backing field required", NAME);

    PsiElementOnlyDiagnosticFactory1<JetSimpleNameExpression, DeclarationDescriptor> FUNCTION_PARAMETERS_OF_INLINE_FUNCTION = PsiElementOnlyDiagnosticFactory1.create(ERROR, "Function parameters of inline function can only be invoked", NAME);

    SimpleDiagnosticFactory UNREACHABLE_CODE = SimpleDiagnosticFactory.create(ERROR, "Unreachable code");

    SimpleDiagnosticFactory MANY_CLASS_OBJECTS = SimpleDiagnosticFactory.create(ERROR, "Only one class object is allowed per class");
    SimpleDiagnosticFactory CLASS_OBJECT_NOT_ALLOWED = SimpleDiagnosticFactory.create(ERROR, "A class object is not allowed here");
    SimpleDiagnosticFactory DELEGATION_IN_TRAIT = SimpleDiagnosticFactory.create(ERROR, "Traits cannot use delegation");
    SimpleDiagnosticFactory DELEGATION_NOT_TO_TRAIT = SimpleDiagnosticFactory.create(ERROR, "Only traits can be delegated to");
    SimpleDiagnosticFactory NO_CONSTRUCTOR = SimpleDiagnosticFactory.create(ERROR, "This class does not have a constructor");
    SimpleDiagnosticFactory NOT_A_CLASS = SimpleDiagnosticFactory.create(ERROR, "Not a class");
    SimpleDiagnosticFactory ILLEGAL_ESCAPE_SEQUENCE = SimpleDiagnosticFactory.create(ERROR, "Illegal escape sequence");

    SimpleDiagnosticFactory LOCAL_EXTENSION_PROPERTY = SimpleDiagnosticFactory.create(ERROR, "Local extension properties are not allowed");
    SimpleDiagnosticFactory LOCAL_VARIABLE_WITH_GETTER = SimpleDiagnosticFactory.create(ERROR, "Local variables are not allowed to have getters");
    SimpleDiagnosticFactory LOCAL_VARIABLE_WITH_SETTER = SimpleDiagnosticFactory.create(ERROR, "Local variables are not allowed to have setters");
    SimplePsiElementOnlyDiagnosticFactory<JetProperty> VAL_WITH_SETTER = SimplePsiElementOnlyDiagnosticFactory.create(ERROR, "A 'val'-property cannot have a setter");

    SimpleDiagnosticFactory EQUALS_MISSING = SimpleDiagnosticFactory.create(ERROR, "No method 'equals(Any?) : Boolean' available");
    SimpleDiagnosticFactory ASSIGNMENT_IN_EXPRESSION_CONTEXT = SimpleDiagnosticFactory.create(ERROR, "Assignments are not expressions, and only expressions are allowed in this context");
    SimpleDiagnosticFactory NAMESPACE_IS_NOT_AN_EXPRESSION = SimpleDiagnosticFactory.create(ERROR, "'namespace' is not an expression, it can only be used on the left-hand side of a dot ('.')");
    ParameterizedDiagnosticFactory1<String> SUPER_IS_NOT_AN_EXPRESSION = ParameterizedDiagnosticFactory1.create(ERROR, "{0} is not an expression, it can only be used on the left-hand side of a dot ('.')");
    SimpleDiagnosticFactory DECLARATION_IN_ILLEGAL_CONTEXT = SimpleDiagnosticFactory.create(ERROR, "Declarations are not allowed in this position");
    SimpleDiagnosticFactory SETTER_PARAMETER_WITH_DEFAULT_VALUE = SimpleDiagnosticFactory.create(ERROR, "Setter parameters can not have default values");
    SimpleDiagnosticFactory NO_THIS = SimpleDiagnosticFactory.create(ERROR, "'this' is not defined in this context");
    SimpleDiagnosticFactory SUPER_NOT_AVAILABLE = SimpleDiagnosticFactory.create(ERROR, "No supertypes are accessible in this context");
    SimpleDiagnosticFactory AMBIGUOUS_SUPER = SimpleDiagnosticFactory.create(ERROR, "Many supertypes available, please specify the one you mean in angle brackets, e.g. 'super<Foo>'");
    SimpleDiagnosticFactory NOT_A_SUPERTYPE = SimpleDiagnosticFactory.create(ERROR, "Not a supertype");
    SimpleDiagnosticFactory TYPE_ARGUMENTS_REDUNDANT_IN_SUPER_QUALIFIER = SimpleDiagnosticFactory.create(WARNING, "Type arguments do not need to be specified in a 'super' qualifier");
    SimpleDiagnosticFactory NO_WHEN_ENTRIES = SimpleDiagnosticFactory.create(ERROR, "Entries are required for when-expression"); // TODO : Scope, and maybe this should not be an error
    SimplePsiElementOnlyDiagnosticFactory<JetBinaryExpressionWithTypeRHS> USELESS_CAST_STATIC_ASSERT_IS_FINE = SimplePsiElementOnlyDiagnosticFactory.create(WARNING, "No cast needed, use ':' instead");
    SimplePsiElementOnlyDiagnosticFactory<JetBinaryExpressionWithTypeRHS> USELESS_CAST = SimplePsiElementOnlyDiagnosticFactory.create(WARNING, "No cast needed");
    SimpleDiagnosticFactory CAST_NEVER_SUCCEEDS = SimpleDiagnosticFactory.create(WARNING, "This cast can never succeed");
    DiagnosticWithParameterFactory<JetPropertyAccessor, JetType> WRONG_SETTER_PARAMETER_TYPE = DiagnosticWithParameterFactory.create(ERROR, "Setter parameter type must be equal to the type of the property, i.e. {0}", DiagnosticParameters.TYPE);
    DiagnosticWithParameterFactory<JetPropertyAccessor, JetType> WRONG_GETTER_RETURN_TYPE = DiagnosticWithParameterFactory.create(ERROR, "Getter return type must be equal to the type of the property, i.e. {0}", DiagnosticParameters.TYPE);
    ParameterizedDiagnosticFactory1<ClassifierDescriptor> NO_CLASS_OBJECT = ParameterizedDiagnosticFactory1.create(ERROR, "Please specify constructor invocation; classifier {0} does not have a class object", NAME);
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
    ParameterizedDiagnosticFactory1<JetType> CALLEE_NOT_A_FUNCTION = ParameterizedDiagnosticFactory1.create(ERROR, "Expecting a function type, but found {0}");

    SimpleDiagnosticFactory RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY = SimpleDiagnosticFactory.create(ERROR, "Returns are not allowed for functions with expression body. Use block body in '{...}'");
    SimpleDiagnosticFactory NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY = SimpleDiagnosticFactory.create(ERROR, "A 'return' expression required in a function with a block body ('{...}')");
    ParameterizedDiagnosticFactory1<JetType> RETURN_TYPE_MISMATCH = ParameterizedDiagnosticFactory1.create(ERROR, "This function must return a value of type {0}");
    ParameterizedDiagnosticFactory1<JetType> EXPECTED_TYPE_MISMATCH = ParameterizedDiagnosticFactory1.create(ERROR, "Expected a value of type {0}");
    ParameterizedDiagnosticFactory1<JetType> ASSIGNMENT_TYPE_MISMATCH = ParameterizedDiagnosticFactory1.create(ERROR, "Expected a value of type {0}. Assignment operation is not an expression, so it does not return any value");
    ParameterizedDiagnosticFactory1<JetType> IMPLICIT_CAST_TO_UNIT_OR_ANY = ParameterizedDiagnosticFactory1.create(WARNING, "Type was casted to ''{0}''. Please specify ''{0}'' as expected type, if you mean such cast");
    ParameterizedDiagnosticFactory1<JetExpression> EXPRESSION_EXPECTED = new ParameterizedDiagnosticFactory1<JetExpression>(ERROR, "{0} is not an expression, and only expression are allowed here") {
        @Override
        protected String makeMessageFor(JetExpression expression) {
            String expressionType = expression.toString();
            return expressionType.substring(0, 1) + expressionType.substring(1).toLowerCase();
        }
    };

    ParameterizedDiagnosticFactory1<JetType> UPPER_BOUND_VIOLATED = ParameterizedDiagnosticFactory1.create(ERROR, "An upper bound {0} is violated"); // TODO : Message
    ParameterizedDiagnosticFactory1<JetType> FINAL_CLASS_OBJECT_UPPER_BOUND = ParameterizedDiagnosticFactory1.create(ERROR, "{0} is a final type, and thus a class object cannot extend it");
    ParameterizedDiagnosticFactory1<JetType> FINAL_UPPER_BOUND = ParameterizedDiagnosticFactory1.create(WARNING, "{0} is a final type, and thus a value of the type parameter is predetermined");
    PsiElementOnlyDiagnosticFactory1<JetBinaryExpression, JetType> USELESS_ELVIS = PsiElementOnlyDiagnosticFactory1.create(WARNING, "Elvis operator (?:) always returns the left operand of non-nullable type {0}");
    ParameterizedDiagnosticFactory1<TypeParameterDescriptor> CONFLICTING_UPPER_BOUNDS = new ParameterizedDiagnosticFactory1<TypeParameterDescriptor>(ERROR, "Upper bounds of {0} have empty intersection") {
        @Override
        protected String makeMessageFor(@NotNull TypeParameterDescriptor argument) {
            return argument.getName();
        }
    };
    ParameterizedDiagnosticFactory1<TypeParameterDescriptor> CONFLICTING_CLASS_OBJECT_UPPER_BOUNDS = ParameterizedDiagnosticFactory1.create(ERROR, "Class object upper bounds of {0} have empty intersection", NAME);

    ParameterizedDiagnosticFactory1<CallableDescriptor> TOO_MANY_ARGUMENTS = ParameterizedDiagnosticFactory1.create(ERROR, "Too many arguments for {0}");
    ParameterizedDiagnosticFactory1<String> ERROR_COMPILE_TIME_VALUE = ParameterizedDiagnosticFactory1.create(ERROR, "{0}");

    SimpleDiagnosticFactoryWithPsiElement<JetWhenEntry> ELSE_MISPLACED_IN_WHEN = new SimpleDiagnosticFactoryWithPsiElement<JetWhenEntry>(ERROR, "'else' entry must be the last one in a when-expression") {
        @NotNull
        @Override
        public TextRange getTextRange(@NotNull JetWhenEntry entry) {
            PsiElement elseKeywordElement = entry.getElseKeywordElement();
            assert elseKeywordElement != null;
            return elseKeywordElement.getTextRange();
        }
    };
    SimpleDiagnosticFactoryWithPsiElement<JetWhenExpression> NO_ELSE_IN_WHEN = new SimpleDiagnosticFactoryWithPsiElement<JetWhenExpression>(ERROR, "'when' expression must contain 'else' branch") {
        @NotNull
        @Override
        public TextRange getTextRange(@NotNull JetWhenExpression element) {
            return element.getWhenKeywordElement().getTextRange();
        }
    };
    SimpleDiagnosticFactoryWithPsiElement<JetWhenConditionInRange> TYPE_MISMATCH_IN_RANGE = new SimpleDiagnosticFactoryWithPsiElement<JetWhenConditionInRange>(ERROR, "Type mismatch: incompatible types of range and element checked in it") {
        @NotNull
        @Override
        public TextRange getTextRange(@NotNull JetWhenConditionInRange condition) {
            return condition.getOperationReference().getTextRange();
        }
    };
    SimpleDiagnosticFactory CYCLIC_INHERITANCE_HIERARCHY = SimpleDiagnosticFactory.create(ERROR, "There's a cycle in the inheritance hierarchy for this type");

    SimpleDiagnosticFactory MANY_CLASSES_IN_SUPERTYPE_LIST = SimpleDiagnosticFactory.create(ERROR, "Only one class may appear in a supertype list");
    SimpleDiagnosticFactory SUPERTYPE_NOT_A_CLASS_OR_TRAIT = SimpleDiagnosticFactory.create(ERROR, "Only classes and traits may serve as supertypes");
    SimpleDiagnosticFactory SUPERTYPE_INITIALIZED_IN_TRAIT = SimpleDiagnosticFactory.create(ERROR, "Traits cannot initialize supertypes");
    SimpleDiagnosticFactory CONSTRUCTOR_IN_TRAIT = SimpleDiagnosticFactory.create(ERROR, "A trait may not have a constructor");
    SimpleDiagnosticFactory SUPERTYPE_APPEARS_TWICE = SimpleDiagnosticFactory.create(ERROR, "A supertype appears twice");
    SimpleDiagnosticFactory FINAL_SUPERTYPE = SimpleDiagnosticFactory.create(ERROR, "This type is final, so it cannot be inherited from");

    ParameterizedDiagnosticFactory1<String> ILLEGAL_SELECTOR = ParameterizedDiagnosticFactory1.create(ERROR, "Expression ''{0}'' cannot be a selector (occur after a dot)");

    SimpleDiagnosticFactory VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION = SimpleDiagnosticFactory.create(ERROR, "A type annotation is required on a value parameter");
    SimpleDiagnosticFactory BREAK_OR_CONTINUE_OUTSIDE_A_LOOP = SimpleDiagnosticFactory.create(ERROR, "'break' and 'continue' are only allowed inside a loop");
    ParameterizedDiagnosticFactory1<String> NOT_A_LOOP_LABEL = ParameterizedDiagnosticFactory1.create(ERROR, "The label ''{0}'' does not denote a loop");
    ParameterizedDiagnosticFactory1<String> NOT_A_RETURN_LABEL = ParameterizedDiagnosticFactory1.create(ERROR, "The label ''{0}'' does not reference to a context from which we can return");

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
    ParameterizedDiagnosticFactory2<JetType, String> AUTOCAST_IMPOSSIBLE = ParameterizedDiagnosticFactory2.create(ERROR, "Automatic cast to {0} is impossible, because {1} could have changed since the is-check");

    ParameterizedDiagnosticFactory2<JetType, JetType> TYPE_MISMATCH_IN_FOR_LOOP = ParameterizedDiagnosticFactory2.create(ERROR, "The loop iterates over values of type {0} but the parameter is declared to be {1}");
    ParameterizedDiagnosticFactory1<JetType> TYPE_MISMATCH_IN_CONDITION = ParameterizedDiagnosticFactory1.create(ERROR, "Condition must be of type Boolean, but was of type {0}");
    ParameterizedDiagnosticFactory2<JetType, Integer> TYPE_MISMATCH_IN_TUPLE_PATTERN = ParameterizedDiagnosticFactory2.create(ERROR, "Type mismatch: subject is of type {0} but the pattern is of type Tuple{1}"); // TODO: message
    ParameterizedDiagnosticFactory2<JetType, JetType> TYPE_MISMATCH_IN_BINDING_PATTERN = ParameterizedDiagnosticFactory2.create(ERROR, "{0} must be a supertype of {1}. Use 'is' to match against {0}");
    ParameterizedDiagnosticFactory2<JetType, JetType> INCOMPATIBLE_TYPES = ParameterizedDiagnosticFactory2.create(ERROR, "Incompatible types: {0} and {1}");
    SimpleDiagnosticFactory EXPECTED_CONDITION = SimpleDiagnosticFactory.create(ERROR, "Expected condition of Boolean type");

    ParameterizedDiagnosticFactory1<JetType> CANNOT_CHECK_FOR_ERASED = ParameterizedDiagnosticFactory1.create(ERROR, "Cannot check for instance of erased type: {0}");
    ParameterizedDiagnosticFactory2<JetType, JetType> UNCHECKED_CAST = ParameterizedDiagnosticFactory2.create(WARNING, "Unchecked cast: {0} to {1}");

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
    ParameterizedDiagnosticFactory2<CallableMemberDescriptor, DeclarationDescriptor> OVERRIDING_FINAL_MEMBER = ParameterizedDiagnosticFactory2.create(ERROR, "{0} in {1} is final and cannot be overridden", NAME);

    ParameterizedDiagnosticFactory2<CallableMemberDescriptor, CallableMemberDescriptor> RETURN_TYPE_MISMATCH_ON_OVERRIDE = new ParameterizedDiagnosticFactory2<CallableMemberDescriptor, CallableMemberDescriptor>(ERROR, "Return type of {0} is not a subtype of the return type overridden member {1}") {
        @NotNull
        @Override
        public TextRange getTextRange(@NotNull Diagnostic diagnostic) {
            PsiElement psiElement = ((DiagnosticWithPsiElement) diagnostic).getPsiElement();
            JetTypeReference returnTypeRef = null;
            ASTNode nameNode = null;
            if (psiElement instanceof JetNamedFunction) {
                JetFunction function = (JetNamedFunction) psiElement;
                returnTypeRef = function.getReturnTypeRef();
                nameNode = getNameNode(function);
            }
            else if (psiElement instanceof JetProperty) {
                JetProperty property = (JetProperty) psiElement;
                returnTypeRef = property.getPropertyTypeRef();
                nameNode = getNameNode(property);
            }
            else if (psiElement instanceof JetPropertyAccessor) {
                JetPropertyAccessor accessor = (JetPropertyAccessor) psiElement;
                returnTypeRef = accessor.getReturnTypeReference();
                nameNode = accessor.getNamePlaceholder().getNode();
            }
            if (returnTypeRef != null) return returnTypeRef.getTextRange();
            if (nameNode != null) return nameNode.getTextRange();
            return super.getTextRange(diagnostic);
        }

        private ASTNode getNameNode(JetNamedDeclaration function) {
            PsiElement nameIdentifier = function.getNameIdentifier();
            return nameIdentifier == null ? null : nameIdentifier.getNode();
        }

        @Override
        protected String makeMessageForA(@NotNull CallableMemberDescriptor callableMemberDescriptor) {
            return DescriptorRenderer.TEXT.render(callableMemberDescriptor);
        }

        @Override
        protected String makeMessageForB(@NotNull CallableMemberDescriptor callableMemberDescriptor) {
            return DescriptorRenderer.TEXT.render(callableMemberDescriptor);
        }
    };

    ParameterizedDiagnosticFactory2<PropertyDescriptor, PropertyDescriptor> VAR_OVERRIDDEN_BY_VAL = new ParameterizedDiagnosticFactory2<PropertyDescriptor, PropertyDescriptor>(ERROR, "Var-property {0} cannot be overridden by val-property {1}", DescriptorRenderer.TEXT);

    ParameterizedDiagnosticFactory2<JetClassOrObject, CallableMemberDescriptor> ABSTRACT_MEMBER_NOT_IMPLEMENTED = new ParameterizedDiagnosticFactory2<JetClassOrObject, CallableMemberDescriptor>(ERROR, "{0} must be declared abstract or implement abstract member {1}") {
        @Override
        protected String makeMessageForA(@NotNull JetClassOrObject jetClassOrObject) {
            String name = jetClassOrObject.getName() != null ? " '" + jetClassOrObject.getName() + "'" : "";
            if (jetClassOrObject instanceof JetClass) {
                return "Class" + name;
            }
            return "Object" + name;
        }

        @Override
        protected String makeMessageForB(@NotNull CallableMemberDescriptor memberDescriptor) {
            return DescriptorRenderer.TEXT.render(memberDescriptor);
        }
    };

    ParameterizedDiagnosticFactory2<JetClassOrObject, CallableMemberDescriptor> MANY_IMPL_MEMBER_NOT_IMPLEMENTED = new ParameterizedDiagnosticFactory2<JetClassOrObject, CallableMemberDescriptor>(ERROR, "Class ''{0}'' must override {1} because it inherits many implementations of it") {
        @Override
        protected String makeMessageForA(@NotNull JetClassOrObject jetClassOrObject) {
            return jetClassOrObject.getName();
        }

        @Override
        protected String makeMessageForB(@NotNull CallableMemberDescriptor memberDescriptor) {
            return DescriptorRenderer.TEXT.render(memberDescriptor);
        }
    };

    FunctionSignatureDiagnosticFactory CONFLICTING_OVERLOADS = new FunctionSignatureDiagnosticFactory(ERROR, "{1} is already defined in ''{0}''");


    ParameterizedDiagnosticFactory3<String, JetType, JetType> RESULT_TYPE_MISMATCH = ParameterizedDiagnosticFactory3.create(ERROR, "{0} must return {1} but returns {2}");
    ParameterizedDiagnosticFactory3<String, String, String> UNSAFE_INFIX_CALL = ParameterizedDiagnosticFactory3.create(ERROR, "Infix call corresponds to a dot-qualified call ''{0}.{1}({2})'' which is not allowed on a nullable receiver ''{0}''. Use '?.'-qualified call instead");

    AmbiguousDescriptorDiagnosticFactory OVERLOAD_RESOLUTION_AMBIGUITY = new AmbiguousDescriptorDiagnosticFactory("Overload resolution ambiguity: {0}");
    AmbiguousDescriptorDiagnosticFactory NONE_APPLICABLE = new AmbiguousDescriptorDiagnosticFactory("None of the following functions can be called with the arguments supplied: {0}");
    ParameterizedDiagnosticFactory1<ValueParameterDescriptor> NO_VALUE_FOR_PARAMETER = ParameterizedDiagnosticFactory1.create(ERROR, "No value passed for parameter {0}", DescriptorRenderer.TEXT);
    ParameterizedDiagnosticFactory1<JetType> MISSING_RECEIVER = ParameterizedDiagnosticFactory1.create(ERROR, "A receiver of type {0} is required");
    SimpleDiagnosticFactory NO_RECEIVER_ADMITTED = SimpleDiagnosticFactory.create(ERROR, "No receiver can be passed to this function or property");

    SimpleDiagnosticFactory CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS = SimpleDiagnosticFactory.create(ERROR, "Can not create an instance of an abstract class");
    ParameterizedDiagnosticFactory1<SolutionStatus> TYPE_INFERENCE_FAILED = ParameterizedDiagnosticFactory1.create(ERROR, "Type inference failed: {0}");
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
