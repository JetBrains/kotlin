// FIR_IDENTICAL
// NI_EXPECTED_FILE
fun <X, Y, Z> foo(f: (Y) -> Z, g: (X) -> Y, x: X): Z = f(g(x))

// TODO: Actually, this is a bug and will work when new inference is enabled
// see ([NI] Select variable with proper non-trivial constraint first) for more details
fun test() = foo({ it + 1 }, { it.length }, "")
