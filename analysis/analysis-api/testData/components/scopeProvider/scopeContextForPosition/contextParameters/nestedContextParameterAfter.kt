interface BaseA
interface Another
class A : BaseA, Another
class B
class C
class D

context(_: A, _: B, _: C, _: D)
fun check() {

}

context(a: T)
fun <T : Another?> usage(action: context(D) (context(B) C.() -> Unit) -> Unit) {
    if (a is BaseA) {
        val d = D()
        with(d) {
            if (a is A) {
                action {
                    check()
                }
            }
        }

        <expr>Unit</expr>
    }
}

// LANGUAGE: +ContextParameters
