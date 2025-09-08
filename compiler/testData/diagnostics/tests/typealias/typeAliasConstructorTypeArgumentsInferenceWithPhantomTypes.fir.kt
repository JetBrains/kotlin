// RUN_PIPELINE_TILL: FRONTEND
// NI_EXPECTED_FILE

class Foo<A : Number>
class Bar<B : CharSequence>

class Hr<A, B, C, D>(val a: A, val b: B)

typealias Test<A, B> = Hr<A, B, Foo<A>, Bar<B>>

val test1 = Test(1, "")
val test2 = <!UPPER_BOUND_VIOLATED!>Test<!>(1, 2)


typealias Bas<T> = Hr<T, T, Foo<T>, Bar<T>>

val test3 = <!UPPER_BOUND_VIOLATED!>Bas<!>(1, 1)

/* GENERATED_FIR_TAGS: classDeclaration, integerLiteral, nullableType, primaryConstructor, propertyDeclaration,
stringLiteral, typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeConstraint, typeParameter */
