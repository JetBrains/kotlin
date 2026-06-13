// ISSUE: KT-85238
// LANGUAGE: +CollectionLiterals +ContextSensitiveResolutionUsingExpectedType
// WITH_STDLIB
// RUN_PIPELINE_TILL: FRONTEND

sealed class C {
    object X : C()

    @Deprecated("", level = ERROR)
    companion object {
        val x: C = X

        operator fun of(vararg ints: Int): C = X
    }
}

typealias T = C

fun useSites() {
    val a: C = <!DEPRECATION_ERROR!>[]<!>
    val b: C = <!DEPRECATION_ERROR!>x<!>
    val c: T = <!DEPRECATION_ERROR!>x<!>
}

class WithHidden {
    @Deprecated("", level = HIDDEN)
    companion object {
        operator fun of(vararg ints: Int): WithHidden = WithHidden()
    }
}

fun test() {
    // should be WithHidden or List?
    // if companion is private, resolves to List
    val c = if (true) WithHidden() else <!DEPRECATION_ERROR!>[1, 2, 3]<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, ifExpression, integerLiteral,
localProperty, nestedClass, objectDeclaration, operator, propertyDeclaration, sealed, stringLiteral, vararg */
