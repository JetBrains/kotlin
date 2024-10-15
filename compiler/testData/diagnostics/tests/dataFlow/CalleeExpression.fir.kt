// RUN_PIPELINE_TILL: BACKEND
class C(val f : () -> Unit)

fun test(e : Any) {
    if (e is C) {
        (e.f)()
    }
}
