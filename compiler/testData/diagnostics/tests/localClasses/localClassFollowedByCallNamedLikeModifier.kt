// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB
// ISSUE: KT-90782

annotation class Ann

fun inline(s: String): Nothing = throw Exception(s)

fun value(f: () -> Unit) {}

fun private(s: String, f: () -> Unit = {}) {}

fun context(s: String) {}

fun localClass(): Int {
    class C
    error("boom")
}

fun localGenericClass(): Int {
    class C<T>
    inline("boom")
}

fun localAbstractClass() {
    abstract class C
    value {}
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
    private("boom") {}
}

fun localClassInLambda() {
    run {
        class C
        inline("boom")
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
