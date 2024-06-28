<!ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_VALUE_CLASS, VALUE_CLASS_WITHOUT_JVM_INLINE_ANNOTATION!>value<!> class WithoutConstructor {}

<!INLINE_CLASS_DEPRECATED!>inline<!> class WithoutParameter<!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE!>()<!> {}
<!INLINE_CLASS_DEPRECATED!>inline<!> class WithTwoParameters<!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE!>(val x: Int, val y: String)<!> {}

<!INLINE_CLASS_DEPRECATED!>inline<!> class Ok(private val x: Int) {}
<!INLINE_CLASS_DEPRECATED!>inline<!> class OpenParameter(<!VALUE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER!><!NON_FINAL_MEMBER_IN_FINAL_CLASS!>open<!> val x: Int<!>) {}
<!INLINE_CLASS_DEPRECATED!>inline<!> class VarargParameter(<!VALUE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER!>vararg x: Int<!>) {}
<!INLINE_CLASS_DEPRECATED!>inline<!> class VarParameter(<!VALUE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER!>var x: Int<!>) {}
<!INLINE_CLASS_DEPRECATED!>inline<!> class SimpleParameter(<!VALUE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER!>x: Int<!>) {}

<!INLINE_CLASS_DEPRECATED!>inline<!> class UnitParameter(val x: <!VALUE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE!>Unit<!>)
<!INLINE_CLASS_DEPRECATED!>inline<!> class NothingParameter(val x: <!VALUE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE!>Nothing<!>)
<!INLINE_CLASS_DEPRECATED!>inline<!> class TypeParameterType<T>(val x: T)
<!INLINE_CLASS_DEPRECATED!>inline<!> class ArrayOfTypeParameters<T>(val x: Array<T>)
<!INLINE_CLASS_DEPRECATED!>inline<!> class ListOfTypeParameters<T>(val x: List<T>)
<!INLINE_CLASS_DEPRECATED!>inline<!> class StarProjection<T>(val x: Array<*>)

<!INLINE_CLASS_DEPRECATED!>inline<!> class SimpleRecursive(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>SimpleRecursive<!>)
<!INLINE_CLASS_DEPRECATED!>inline<!> class DoubleRecursive1(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>DoubleRecursive2<!>)
<!INLINE_CLASS_DEPRECATED!>inline<!> class DoubleRecursive2(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>DoubleRecursive1<!>)
