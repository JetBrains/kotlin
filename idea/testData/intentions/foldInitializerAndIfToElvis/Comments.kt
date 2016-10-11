fun foo(p: List<String?>): Int {
    val v = p[0]
    // now check if v is null
    <caret>if (v == null/* null */) return -1 // return -1
    return v.length
}