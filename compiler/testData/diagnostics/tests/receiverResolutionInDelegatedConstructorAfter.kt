// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// LANGUAGE: +ImprovedResolutionInSecondaryConstructors
// ISSUE: KT-48870

open class Base(p: Any?) {
    open fun foo1() {}
}

class Vase(p: Any?) : Base(p) {
    override fun foo1() {}
}

fun Vase.test1() {
    class B : Base {
        // FIR: OK, it's this@foo.foo1()
        // FE 1.0: INSTANCE_ACCESS_BEFORE_SUPER_CALL
        constructor() : super(foo1())
    }
}

fun Base.test2() {
    class B : Base(foo1()) {}
}

open class BaseLambda(lambda: () -> Any?) {
    fun foo1() {}
}

fun Base.test3() {
    class B : Base {
        constructor() : super({ foo1() })
    }
}

fun Base.test4() {
    class B : Base({ foo1() }) {}
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType, lambdaLiteral,
localClass, nullableType, override, primaryConstructor, secondaryConstructor */
