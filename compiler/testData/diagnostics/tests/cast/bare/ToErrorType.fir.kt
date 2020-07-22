class P

fun foo(p: P): Any {
    val v = p as <!OTHER_ERROR, OTHER_ERROR, OTHER_ERROR, OTHER_ERROR, OTHER_ERROR!>G<!>
    return v
}