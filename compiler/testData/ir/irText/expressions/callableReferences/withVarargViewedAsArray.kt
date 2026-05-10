fun sum(vararg args: Int): Int {
    var result = 0
    for (arg in args)
        result += arg
    return result
}

fun nsum(vararg args: Number) = sum(*IntArray(args.size) { args[it].toInt() })

fun zap(vararg b: String, k: Int = 42) {}

fun usePlainArgs(fn: (Int, Int) -> Int) {}
fun usePrimitiveArray(fn: (IntArray) -> Int) {}
fun useArray(fn: (Array<Int>) -> Int) {}
fun useStringArray(fn: (Array<String>) -> Unit) {}

fun testPlainArgs() { usePlainArgs(::sum) }
fun testPrimitiveArrayAsVararg() { usePrimitiveArray(::sum) }
fun testArrayAsVararg() { useArray(::nsum) }
fun testArrayAndDefaults() { useStringArray(::zap) }
