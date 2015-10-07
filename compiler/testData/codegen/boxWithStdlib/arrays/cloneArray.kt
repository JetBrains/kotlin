import java.util.Arrays.equals

fun box(): String {
    val s = arrayOf("live", "long")
    val t: Array<String> = s.clone()
    if (!equals(s, t)) return "Fail string"
    if (s identityEquals t) return "Fail string identity"

    val ss = arrayOf(s, s)
    val tt: Array<Array<String>> = ss.clone()
    if (!equals(ss, tt)) return "Fail string[]"
    if (ss identityEquals tt) return "Fail string[] identity"

    return "OK"
}
