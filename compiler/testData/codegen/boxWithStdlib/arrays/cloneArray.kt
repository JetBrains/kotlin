import java.util.Arrays.equals

fun box(): String {
    val s = array("live", "long")
    val t = s.clone()
    t : Array<String>
    if (!equals(s, t)) return "Fail string"
    if (s identityEquals t) return "Fail string identity"

    val ss = array(s, s)
    val tt = ss.clone()
    tt : Array<Array<String>>
    if (!equals(ss, tt)) return "Fail string[]"
    if (ss identityEquals tt) return "Fail string[] identity"

    return "OK"
}
