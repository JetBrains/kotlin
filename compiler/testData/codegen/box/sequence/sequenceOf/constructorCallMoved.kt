// WITH_STDLIB
var created = 0
class C { init { created++ } }
fun box(): String {
    val s = sequenceOf(C())
    s.forEach { }
    if (created != 1) return "Fail: created=$created"
    val t = sequenceOf(Any())
    if (t.first() !== t.first()) return "Fail: identity"
    var strings = 0
    val o = object { override fun toString(): String { strings++; return "o" } }
    val u = sequenceOf("$o")
    u.forEach { }
    return if (strings == 1) "OK" else "Fail: strings=$strings"
}
