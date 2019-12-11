fun bar(s: Any): Int {
    return s.hashCode()
}

class MyClass(var p: Any) {
    fun foo(): Int {
        p = "xyz"
        if (p is String) {
            return bar(p)
        }
        return -1
    }
}
