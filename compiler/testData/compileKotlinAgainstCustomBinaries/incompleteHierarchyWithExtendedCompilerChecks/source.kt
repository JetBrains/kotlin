import test.Sub as MySub
import test.SubKt

typealias Sub = MySub

class Test<T>

@Suppress("UNUSED_PARAMETER")
fun useCallRef(ref: Any?) {}

@Suppress("UNUSED_PARAMETER")
fun simpleFun(arg: Sub): Sub = Sub()

inline fun <reified T> inlineFun(t: T) = t

fun test() {
    @Suppress("UNUSED_VARIABLE")
    val x: Sub = Sub()
    Test<Sub>()
    useCallRef(::Sub)
    simpleFun(Sub())
    inlineFun<Sub>(Sub())
    SubKt.companionMethod()
    SubKt.InnerObject.objectMethod()
}
