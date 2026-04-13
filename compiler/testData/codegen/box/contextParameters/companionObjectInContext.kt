// IGNORE_BACKEND: ANDROID
// LANGUAGE: +ContextParameters

class MyClass {
    companion object {
        fun foo(): String { return "OK" }
    }
}

fun <A, R> context(context: A, block: context(A) () -> R): R = block(context)

context(a: MyClass.Companion)
fun funWithCompanionContext(): String { return a.foo() }

fun box(): String {
    return context(MyClass) {
        funWithCompanionContext()
    }
}
