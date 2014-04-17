fun foo(a: Int, b: Int, c: Int, d: Int) : Boolean {
    return !(a == b) && !(b <caret>== c) && !(a < d)
}