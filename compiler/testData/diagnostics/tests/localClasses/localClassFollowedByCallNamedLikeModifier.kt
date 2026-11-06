// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// ISSUE: KT-90782

annotation class Ann

fun inline(s: String): Nothing = throw Exception(s)

fun value(f: () -> Unit) {}

fun private(s: String, f: () -> Unit = {}) {}

fun context(s: String) {}

fun localClass(): Int {
    class C
    <!WRONG_MODIFIER_TARGET!>error<!>(<!SYNTAX!>"<!><!SYNTAX!>boom<!><!SYNTAX!><!SYNTAX!><!>"<!><!SYNTAX!>)<!>
}

fun localGenericClass(): Int {
    class C<T>
    <!WRONG_MODIFIER_TARGET!>inline<!>(<!SYNTAX!>"<!><!SYNTAX!>boom<!><!SYNTAX!><!SYNTAX!><!>"<!><!SYNTAX!>)<!>
}

fun localAbstractClass() {
    abstract class C
    <!MISSING_CONSTRUCTOR_KEYWORD, WRONG_MODIFIER_TARGET!>value<!><!SYNTAX!><!> {}
}

fun localDataClassWithParameters() {
    data class C(val x: Int)
    private("boom")
}

fun localClassFollowedByCallNamedContext() {
    class C
    context("boom")
}

fun localClassFollowedByCallWithArgumentsAndTrailingLambda() {
    class C
    private(<!SYNTAX!>"<!><!SYNTAX!>boom<!><!SYNTAX!><!SYNTAX!><!>"<!><!SYNTAX!>)<!> {}
}

fun localClassInLambda() {
    run {
        class C
        <!WRONG_MODIFIER_TARGET!>inline<!>(<!SYNTAX!>"<!><!SYNTAX!>boom<!><!SYNTAX!><!SYNTAX!><!>"<!><!SYNTAX!>)<!>
    }
}

fun localClassWithConstructorOnNextLine() {
    class C1
    private constructor()

    class C2
    @Ann constructor()

    class C3<T>
    internal constructor(x: T)
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, data, functionDeclaration, functionalType, lambdaLiteral,
localClass, nullableType, primaryConstructor, propertyDeclaration, stringLiteral, typeParameter */
