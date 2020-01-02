interface Trait {
    fun bar() = 42
}

class Outer : Trait {
    fun foo() {
        val t = this@Outer
        val s = super@Outer.bar()
        
        class Local : Trait {
            val t = this@Outer
            val s = super@Outer.bar()
            
            inner class Inner {
                val t = this@Local
                val s = super@Local.<!UNRESOLVED_REFERENCE!>bar<!>()
                
                val tt = this@Outer
                val ss = super@Outer.<!UNRESOLVED_REFERENCE!>bar<!>()
            }
        }
    }
}
