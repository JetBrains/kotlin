// TARGET_BACKEND: JVM
// TARGET_BACKEND: JVM_IR

// WITH_STDLIB

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
