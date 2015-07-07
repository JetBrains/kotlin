import kotlin.reflect.KFunction

// Initiate descriptor computation in reflection to ensure that nothing fails
fun test(f: KFunction<*>) {
    f.parameters
}

fun box(): String {
    test(J::simple)
    test(J::objectTypes)
    test(J::primitives)
    test(J::primitiveArrays)
    test(J::multiDimensionalArrays)
    test(J::wildcards)

    return "OK"
}
