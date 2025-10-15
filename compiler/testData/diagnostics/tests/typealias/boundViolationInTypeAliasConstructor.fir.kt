// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: FRONTEND
class Num<T: Number>(val x: T)

typealias N<T> = Num<T>
typealias N2<T> = N<T>

val x1 = Num<<!UPPER_BOUND_VIOLATED, UPPER_BOUND_VIOLATED_DEPRECATION_WARNING!>String<!>>("")
val x2 = N<<!UPPER_BOUND_VIOLATED, UPPER_BOUND_VIOLATED_DEPRECATION_WARNING!>String<!>>("")
val x3 = N2<<!UPPER_BOUND_VIOLATED, UPPER_BOUND_VIOLATED_DEPRECATION_WARNING!>String<!>>("")

class TColl<T, C : Collection<T>>

typealias TC<T, C> = TColl<T, C>
typealias TC2<T, C> = TC<T, C>

val y1 = TColl<Any, <!UPPER_BOUND_VIOLATED, UPPER_BOUND_VIOLATED_DEPRECATION_WARNING!>Any<!>>()
val y2 = TC<Any, <!UPPER_BOUND_VIOLATED, UPPER_BOUND_VIOLATED_DEPRECATION_WARNING!>Any<!>>()
val y3 = TC2<Any, <!UPPER_BOUND_VIOLATED, UPPER_BOUND_VIOLATED_DEPRECATION_WARNING!>Any<!>>()

/* GENERATED_FIR_TAGS: classDeclaration, nullableType, primaryConstructor, propertyDeclaration, stringLiteral,
typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeConstraint, typeParameter */
