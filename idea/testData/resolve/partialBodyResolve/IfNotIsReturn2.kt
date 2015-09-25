class C(public val v: Any)

fun foo(c: C) {
    if (c.v !is String) return
    println(c.<caret>v.length())
}