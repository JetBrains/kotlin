// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters
class A

fun funWithContextAndValueType(x: context(A) (Int) -> Unit) {
    x(A(), 1)
    with(A()) {
        x(2)
    }

}
fun funWithContextAndExtensionType(x: context(A) Int.() -> Unit) {
    x(A(), 1)
    with(A()) {
        x(2)
        3.x()
    }
}
fun funWithContextsType(x: context(A, Int) () -> Unit) {
    x(A(), 1)
    with(A()) {
        with(1){
            x()
        }
    }
}