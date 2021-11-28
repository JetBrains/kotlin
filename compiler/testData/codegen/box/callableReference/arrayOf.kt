fun use(fn: (Array<String>) -> Array<String>) =
    fn(arrayOf("OK"))

fun box(): String {
    return use(::arrayOf)[0]
}
