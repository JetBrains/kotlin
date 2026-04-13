// RUN_PIPELINE_TILL: FRONTEND
class Num<T: Number>(val x: T)

typealias N<T> = Num<T>
typealias N2<T> = N<T>

val x1 = <!INAPPLICABLE_CANDIDATE!>Num<!><<!UPPER_BOUND_VIOLATED!>String<!>>("")
val x2 = <!INAPPLICABLE_CANDIDATE!>N<!><<!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION!>String<!>>("")
val x3 = <!INAPPLICABLE_CANDIDATE!>N2<!><<!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION!>String<!>>("")

class TColl<T, C : Collection<T>>

typealias TC<T, C> = TColl<T, C>
typealias TC2<T, C> = TC<T, C>

val y1 = TColl<Any, <!UPPER_BOUND_VIOLATED!>Any<!>>()
val y2 = TC<Any, <!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION!>Any<!>>()
val y3 = TC2<Any, <!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION!>Any<!>>()

/* GENERATED_FIR_TAGS: classDeclaration, nullableType, primaryConstructor, propertyDeclaration, stringLiteral,
typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeConstraint, typeParameter */
