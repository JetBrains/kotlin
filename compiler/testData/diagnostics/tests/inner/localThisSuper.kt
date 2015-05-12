interface Trait {
    fun bar() = 42
}

class Outer : Trait {
    fun foo() {
        val <!UNUSED_VARIABLE!>t<!> = this@Outer
        val <!UNUSED_VARIABLE!>s<!> = super@Outer.bar()
        
        class Local : Trait {
            val t = this@Outer
            val s = super@Outer.bar()
            
            inner class Inner {
                val t = this@Local
                val s = super@Local.bar()
                
                val tt = this@Outer
                val ss = super@Outer.bar()
            }
        }
    }
}
