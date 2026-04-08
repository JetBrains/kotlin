// IGNORE_BACKEND: ANDROID
// LANGUAGE: +ContextParameters
class A(val a: String)
class B(val b: String)

fun box(): String {
    context(a: A)
    fun B.localFun(): String {
        return this.b + a.a
    }

    with(A("K")) {
        with(B("O")) {
            return localFun()
        }
    }
}
