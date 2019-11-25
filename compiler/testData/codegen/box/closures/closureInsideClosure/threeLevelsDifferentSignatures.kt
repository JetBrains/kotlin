// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    fun foo(x: Int, s: String): String {
        fun bar(y: Int) {
            fun baz(z: Int, k: Int) {
                foo(x - 1 + k, "Fail")
            }
            
            baz(y, 0)
        }
        
        if (x > 0) bar(x)
        return s
    }
    
    return foo(1, "OK")
}
