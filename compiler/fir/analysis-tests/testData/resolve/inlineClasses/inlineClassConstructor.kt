<!ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_VALUE_CLASS, VALUE_CLASS_WITHOUT_JVM_INLINE_ANNOTATION!>value<!> class WithoutConstructor {}

inline class WithoutParameter<!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE!>()<!> {}
inline class WithTwoParameters<!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE!>(val x: Int, val y: String)<!> {}

inline class Ok(private val x: Int) {}
inline class OpenParameter(<!VALUE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER!><!NON_FINAL_MEMBER_IN_FINAL_CLASS!>open<!> val x: Int<!>) {}
inline class VarargParameter(<!VALUE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER!>vararg x: Int<!>) {}
inline class VarParameter(<!VALUE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER!>var x: Int<!>) {}
inline class SimpleParameter(<!VALUE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER!>x: Int<!>) {}

inline class UnitParameter(val x: <!VALUE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE!>Unit<!>)
inline class NothingParameter(val x: <!VALUE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE!>Nothing<!>)
inline class TypeParameterType<T>(val x: T)
inline class ArrayOfTypeParameters<T>(val x: Array<T>)
inline class ListOfTypeParameters<T>(val x: List<T>)
inline class StarProjection<T>(val x: Array<*>)

inline class SimpleRecursive(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>SimpleRecursive<!>)
inline class DoubleRecursive1(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>DoubleRecursive2<!>)
inline class DoubleRecursive2(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>DoubleRecursive1<!>)
