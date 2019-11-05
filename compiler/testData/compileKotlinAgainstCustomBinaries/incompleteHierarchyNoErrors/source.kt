import test.Sub as MySub;

typealias Sub = MySub

class Test<T>

@Suppress("UNUSED_PARAMETER")
fun useCallRef(ref: Any?) {}

@Suppress("UNUSED_PARAMETER")
fun simpleFun(arg: Sub): Sub = Sub()

inline fun <reified T> inlineFun(t: T) = t

// Imports, aliases type references, constructor calls and callable references don't trigger supertype resolution.
// There should be no error for backward compatibility, despite the missing supertype.
fun test() {
    @Suppress("UNUSED_VARIABLE")
    val x: Sub = Sub()
    Test<Sub>()
    useCallRef(::Sub)
    simpleFun(Sub())
    inlineFun<Sub>(Sub())
}
