fun Any?.doSomething() {}

fun bar(): Nothing = throw Exception()

fun foo() {
    null!!.doSomething()
    bar().doSomething()
}