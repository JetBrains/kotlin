interface IA
interface IB
interface IC

object Host : IB

object Prop : IA {
    val Host.foo: Callee get() = Callee
}

object Callee

object Invoke : IC {
    operator fun Callee.invoke() { }
}

fun test(a: IA, b: IB, c: IC) {
    with(a) lambdaA@{
        with(b) lambdaB@{
            with(c) lambdaC@{
                if (this@lambdaA is Prop && this@lambdaB is Host && this@lambdaC is Invoke) {
                    <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>foo<!>()
                }
            }
        }
    }
}
