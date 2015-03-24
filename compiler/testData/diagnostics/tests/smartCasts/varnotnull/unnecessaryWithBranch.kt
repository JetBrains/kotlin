fun String?.foo(): String {
    return this ?: ""
}

class MyClass {
    fun bar(): String {
        var s: String? = null
        if (4 < 2)
            s = "42"
        return s.foo()
    }    
}