// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: SPREAD_OPERATOR
// !LANGUAGE: +NewInference

fun sum(vararg args: Int): Int {
    var result = 0
    for (arg in args)
        result += arg
    return result
}

fun nsum(vararg args: Number) = sum(*IntArray(args.size) { args[it].toInt() })

fun usePlainArgs(fn: (Int, Int) -> Int) = fn(1, 1)
fun usePrimitiveArray(fn: (IntArray) -> Int) = fn(intArrayOf(1, 1, 1))
fun useArray(fn: (Array<Int>) -> Int) = fn(arrayOf(1, 1, 1, 1))

fun box(): String {
    var result = usePlainArgs(::sum)
    if (result != 2)
        return "Fail: plain args $result != 2"
    result = usePrimitiveArray(::sum)
    if (result != 3)
        return "Fail: primitive array $result != 3"
    result = useArray(::nsum)
    if (result != 4)
        return "Fail: reference array $result != 4"
    return "OK"
}
