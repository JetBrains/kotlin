fun Any.nothing(): Nothing {
    while (true) {}
}

class Anything

fun testFunction1(obj: Any?): Nothing? = obj?.nothing()
fun testFunction2(obj: Any?): Any? = obj?.nothing()
fun testFunction3(obj: Any?): String? = obj?.nothing()
fun testFunction4(obj: Any?): Unit? = obj?.nothing()
fun testFunction5(obj: Any?): Anything? = obj?.nothing()

fun testLambda1() {
    val block: (Any?) -> Nothing? = {
        it?.nothing()
    }
    block(null)
}

fun foo(s: String) {}

fun testLambda2() {
    val block: (Any?) -> Nothing? = {
        foo("") // more than one statement inside of the body
        it?.nothing()
    }
    block(null)
}

fun box(): String {
    testFunction1(null)
    testFunction2(null)
    testFunction3(null)
    testFunction4(null)
    testFunction5(null)

    testLambda1()
    testLambda2()

    return "OK"
}
