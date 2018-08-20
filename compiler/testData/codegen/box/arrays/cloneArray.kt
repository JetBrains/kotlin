// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME

fun box(): String {
    val s = arrayOf("live", "long")
    val t: Array<String> = s.clone()
    if (!(s contentEquals t)) return "Fail string"
    if (s === t) return "Fail string identity"

    val ss = arrayOf(s, s)
    val tt: Array<Array<String>> = ss.clone()
    if (!(ss contentEquals tt)) return "Fail string[]"
    if (ss === tt) return "Fail string[] identity"

    return "OK"
}
