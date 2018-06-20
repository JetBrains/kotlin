fun <T> T.toPrefixedString(prefix: String = "", suffix: String="") = prefix + this.toString() + suffix

fun box() : String {
    if("mama".toPrefixedString(suffix="321", prefix="papa") != "papamama321") return "fail"
    if("mama".toPrefixedString(prefix="papa") != "papamama") return "fail"
    if("mama".toPrefixedString("papa", "239") != "papamama239") return "fail"
    if("mama".toPrefixedString("papa") != "papamama") return "fail"
    if("mama".toPrefixedString() != "mama") return "fail"
    return "OK"
}
