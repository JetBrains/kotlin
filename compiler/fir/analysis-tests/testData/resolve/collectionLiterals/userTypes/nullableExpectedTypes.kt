// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals

class C<T> {
    companion object {
        operator fun <K> of(vararg ks: K): C<K> = C()

        fun <G> nullable(): C<G>? = C()
        fun <J> regular(): C<J> = C()
    }
}

typealias D<U> = C<U>?

fun expectNullable(arg: C<String>?) {}
fun <K> expectNullableGeneric(arg: C<K>?) {}
fun <L> expectThroughTypealias(arg: D<L>) {}
fun <H> expectThroughTV(a: H, b: H) {}
fun <I> expectThroughNullableTV(a: I, b: I?) {}

fun test() {
    expectNullable([])
    expectNullable(["42"])
    expectNullable(<!ARGUMENT_TYPE_MISMATCH!>[42]<!>)

    <!CANNOT_INFER_PARAMETER_TYPE!>expectNullableGeneric<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    expectNullableGeneric(["42"])
    expectNullableGeneric([42, "42"])
    expectNullableGeneric<String>([])

    expectThroughTypealias([42])
    expectThroughTypealias([42, "42"])
    expectThroughTypealias<String>([])
    expectThroughTypealias<String>(<!ARGUMENT_TYPE_MISMATCH!>[42]<!>)

    expectThroughTV(C.nullable(), [42])
    expectThroughTV(C.nullable<Int>(), [42])
    expectThroughTV(C.nullable<String>(), [42])
    expectThroughTV(C.nullable<String>(), [])

    expectThroughNullableTV(C.regular(), [42])
    expectThroughNullableTV(C.regular<Int>(), [42])
    expectThroughNullableTV(C.regular<String>(), [42])
    expectThroughNullableTV(C.regular<String>(), [])
}

/* GENERATED_FIR_TAGS: classDeclaration, collectionLiteral, companionObject, functionDeclaration, integerLiteral,
intersectionType, nullableType, objectDeclaration, operator, stringLiteral, typeAliasDeclaration,
typeAliasDeclarationWithTypeParameter, typeParameter, vararg */
