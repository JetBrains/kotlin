// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-80492
// LANGUAGE: +CollectionLiterals

class C<T> {
    companion object {
        <!MULTIPLE_VARARG_OVERLOADS_OF_OPERATOR_OF!>operator fun of(vararg strs: String): C<String><!> = C<String>()
        <!MULTIPLE_VARARG_OVERLOADS_OF_OPERATOR_OF!>operator fun of(vararg ints: Int): C<Int><!> = C<Int>()
    }
}

class D<U> {
    companion object {
        <!NO_VARARG_OVERLOAD_OF_OPERATOR_OF!>operator fun of(str: String): D<String><!> = D<String>()
        <!NO_VARARG_OVERLOAD_OF_OPERATOR_OF!>operator fun of(): D<String><!> = D<String>()
    }
}

class E<S> {
    companion object {
        <!MULTIPLE_VARARG_OVERLOADS_OF_OPERATOR_OF!>operator fun <S> of(vararg s: S): E<S><!> = E()
        <!MULTIPLE_VARARG_OVERLOADS_OF_OPERATOR_OF!>operator fun of(vararg str: String): E<String><!> = E()
        operator fun of(): E<String> = E()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, nullableType, objectDeclaration, override,
typeParameter, vararg */
