// NI_EXPECTED_FILE

interface Trait {
    fun bar() = 42
}

class Outer : Trait {
    class Nested {
        val t = this@Outer.bar()
        val s = super@Outer.bar()
        
        inner class NestedInner {
            val t = this@Outer.bar()
            val s = super@Outer.bar()
        }
    }
    
    inner class Inner {
        val t = this@Outer.bar()
        val s = super@Outer.bar()
    }
}