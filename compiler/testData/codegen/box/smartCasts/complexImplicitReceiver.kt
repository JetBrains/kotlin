interface Bound
interface CompilerPhase<in X1 : Bound, Y1>

private class CompositePhase<X2 : Bound, Y2>(
    val foo: String
) : CompilerPhase<X2, Y2>

@Suppress("UNCHECKED_CAST")
fun <X3 : Bound, Y3> CompilerPhase<X3, Y3>.bar(): String {
    this as CompilerPhase<X3, Any?>
    val ok = if (this is CompositePhase<X3, *>) foo + "K" else "fail"
    return ok
}

fun box(): String {
    return CompositePhase<Bound, Int>("O").bar()
}
