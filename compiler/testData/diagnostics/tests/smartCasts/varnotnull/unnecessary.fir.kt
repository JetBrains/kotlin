fun String?.foo(): String {
    return this ?: ""
}

class MyClass {
    private var s: String? = null

    fun bar(): String {
        s = "42"
        return s.foo()
    }    
}