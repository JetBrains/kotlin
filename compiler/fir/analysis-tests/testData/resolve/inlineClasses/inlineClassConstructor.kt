<!ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_INLINE_CLASS!>value<!> class WithoutConstructor {}

inline class WithoutParameter<!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE!>()<!> {}
inline class WithTwoParameters<!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE!>(val x: Int, val y: String)<!> {}

inline class Ok(private val x: Int) {}
inline class OpenParameter(<!INLINE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER!><!NON_FINAL_MEMBER_IN_FINAL_CLASS!>open<!> val x: Int<!>) {}
inline class VarargParameter(<!INLINE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER!>vararg x: Int<!>) {}
inline class VarParameter(<!INLINE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER!>var x: Int<!>) {}
inline class SimpleParameter(<!INLINE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER!>x: Int<!>) {}

inline class UnitParameter(val x: <!INLINE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE!>Unit<!>)
inline class NothingParameter(val x: <!INLINE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE!>Nothing<!>)
inline class TypeParameterType<T>(val x: <!INLINE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE!>T<!>)
inline class ArrayOfTypeParameters<T>(val x: <!INLINE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE!>Array<T><!>)
inline class ListOfTypeParameters<T>(val x: List<T>)
inline class StarProjection<T>(val x: Array<*>)

inline class SimpleRecursive(val x: <!INLINE_CLASS_CANNOT_BE_RECURSIVE!>SimpleRecursive<!>)
inline class DoubleRecursive1(val x: <!INLINE_CLASS_CANNOT_BE_RECURSIVE!>DoubleRecursive2<!>)
inline class DoubleRecursive2(val x: <!INLINE_CLASS_CANNOT_BE_RECURSIVE!>DoubleRecursive1<!>)
