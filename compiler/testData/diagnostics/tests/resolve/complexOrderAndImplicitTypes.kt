// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-66313

val foo: String get() = ""

class Test1 {
    private val otherFoo = foo

    fun getFoo() = otherFoo
}

class Test2 {
    fun getFoo() = otherFoo

    private val otherFoo = foo
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, getter, propertyDeclaration, stringLiteral */
