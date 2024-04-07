// ISSUE: KT-66148

class Controller<E>

fun <E1> generate(block: Controller<E1>.() -> Unit) {
    block(Controller())
}

class A(val r: String)

fun foo(c: Controller<String>): A = A("OK")

fun bar(
    propertyForInvoke: A.() -> Unit,
) {
    generate {
        foo(this).propertyForInvoke()
    }
}

fun box(): String {
    var result = "fail"
    bar {
        result = r
    }

    return result
}
