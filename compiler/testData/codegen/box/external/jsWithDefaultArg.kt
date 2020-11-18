// TARGET_BACKEND: JS_IR
// CALL_MAIN

external fun create(
    p0: String = definedExternally,
    p1: String = definedExternally,
    p2: String = definedExternally,
    p3: String = definedExternally,
    p4: String = definedExternally,
) : Array<String>

fun main() {
    js("global.create = function() {return arguments}")
}

fun box(): String {
    val zeroArgs = create()
    if (zeroArgs.size != 0) return "fail: $zeroArgs arguments instead 0"

    val p2 = "p2"
    val threeArgs = create(p2 = p2)
    if (threeArgs.size != 3 || threeArgs[2] != p2) return "fail: $threeArgs arguments instead 3"

    return "OK"
}
