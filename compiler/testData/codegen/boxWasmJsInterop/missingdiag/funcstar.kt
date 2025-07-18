// leads to "IllegalArgumentException: Star projection is not supported in function type interop ..."
external var func : Function1<*,*>?

fun box(): String {
    return "OK"
}