// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

fun foo(): String = "1"

fun test(a: String) {}
fun test2(a: () -> String) {}
fun test3(vararg a: String) {}
fun <T> test4(a: T) {}

fun getArgs(): Array<String> = arrayOf("1", "1")

fun usage() {
    test(foo())
    test(a = foo())
    test2 {
        foo()
    }
    test3(foo(), foo(), foo())
    test3(*getArgs())
    test4(foo())

    fun local(a: String) {}
    local(foo())

    fun(): String { return foo() }

    val anonim = fun(a: String) {}
    anonim(foo())
}

/* GENERATED_FIR_TAGS: anonymousFunction, functionDeclaration, functionalType, lambdaLiteral, localFunction,
localProperty, nullableType, propertyDeclaration, stringLiteral, typeParameter, vararg */
