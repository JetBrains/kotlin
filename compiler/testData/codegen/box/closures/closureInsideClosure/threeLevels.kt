// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    fun foo(x: Int) {
        fun bar(y: Int) {
            fun baz(z: Int) {
                foo(x - 1)
            }
            
            baz(y)
        }
        
        if (x > 0) bar(x)
    }
    
    foo(1)
    
    return "OK"
}
