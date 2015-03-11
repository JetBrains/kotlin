class A {
    val x: Int = 42
    
    fun foo(): String = ""
    
    default object {
        val y: Any? = 239
        
        fun bar(): String = ""
    }
}

fun baz(): String = ""
