fun foo(p: Int): Boolean {
    if (p < 0 <caret>|| p == 5) return false
    return true
}