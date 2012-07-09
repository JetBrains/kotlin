class A {
    class object {
        val b = 0
        val c = b
        
        {
            val d = b
        }
    }
}

fun box(): String {
    A()
    return if (A.c == A.b) "OK" else "Fail"
}
