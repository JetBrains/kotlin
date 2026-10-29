// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE_FEATURE_TOGGLED: ExplicitInlineValueClassesSyntaxRequiredOnAllPlatforms
<!ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_VALUE_CLASS!>value<!> class WithoutConstructor {}

<!INLINE_CLASS_DEPRECATED_ERROR!>inline<!> class WithoutParameter<!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE!>()<!> {}
<!INLINE_CLASS_DEPRECATED_ERROR!>inline<!> class WithTwoParameters<!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE!>(val x: Int, val y: String)<!> {}

<!INLINE_CLASS_DEPRECATED_ERROR!>inline<!> class Ok(private val x: Int) {}
<!INLINE_CLASS_DEPRECATED_ERROR!>inline<!> class OpenParameter(<!VALUE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER!><!NON_FINAL_MEMBER_IN_FINAL_CLASS!>open<!> val x: Int<!>) {}
<!INLINE_CLASS_DEPRECATED_ERROR!>inline<!> class VarargParameter(<!VALUE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER!>vararg x: Int<!>) {}
<!INLINE_CLASS_DEPRECATED_ERROR!>inline<!> class VarParameter(<!VALUE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER!>var x: Int<!>) {}
<!INLINE_CLASS_DEPRECATED_ERROR!>inline<!> class SimpleParameter(<!VALUE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER!>x: Int<!>) {}

<!INLINE_CLASS_DEPRECATED_ERROR!>inline<!> class UnitParameter(val x: <!VALUE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE!>Unit<!>)
<!INLINE_CLASS_DEPRECATED_ERROR!>inline<!> class NothingParameter(val x: <!VALUE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE!>Nothing<!>)
<!INLINE_CLASS_DEPRECATED_ERROR!>inline<!> class TypeParameterType<T>(val x: T)
<!INLINE_CLASS_DEPRECATED_ERROR!>inline<!> class ArrayOfTypeParameters<T>(val x: Array<T>)
<!INLINE_CLASS_DEPRECATED_ERROR!>inline<!> class ListOfTypeParameters<T>(val x: List<T>)
<!INLINE_CLASS_DEPRECATED_ERROR!>inline<!> class StarProjection<T>(val x: Array<*>)

<!INLINE_CLASS_DEPRECATED_ERROR!>inline<!> class SimpleRecursive(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>SimpleRecursive<!>)
<!INLINE_CLASS_DEPRECATED_ERROR!>inline<!> class DoubleRecursive1(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>DoubleRecursive2<!>)
<!INLINE_CLASS_DEPRECATED_ERROR!>inline<!> class DoubleRecursive2(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>DoubleRecursive1<!>)

/* GENERATED_FIR_TAGS: classDeclaration, nullableType, primaryConstructor, propertyDeclaration, starProjection,
typeParameter, value, vararg */
