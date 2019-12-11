class Outer {
    fun foo(): Int {
        if (outerState > 0) return outerState
        
        class Local {
            val localState = <!UNRESOLVED_REFERENCE!>outerState<!>
            
            inner class LocalInner {
                val o = <!UNRESOLVED_REFERENCE!>outerState<!>
                val l = localState
            }
        }
        
        return Local().localState
    }
    
    val outerState = 42
}
