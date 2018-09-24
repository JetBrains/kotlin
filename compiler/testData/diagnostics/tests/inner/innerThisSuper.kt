// NI_EXPECTED_FILE

interface Trait {
    fun bar() = 42
}

class Outer : Trait {
    class Nested {
        val t = this<!UNRESOLVED_REFERENCE!>@Outer<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>bar<!>()
        val s = <!DEBUG_INFO_MISSING_UNRESOLVED!>super<!><!UNRESOLVED_REFERENCE!>@Outer<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>bar<!>()
        
        inner class NestedInner {
            val t = this<!UNRESOLVED_REFERENCE!>@Outer<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>bar<!>()
            val s = <!DEBUG_INFO_MISSING_UNRESOLVED!>super<!><!UNRESOLVED_REFERENCE!>@Outer<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>bar<!>()
        }
    }
    
    inner class Inner {
        val t = this@Outer.bar()
        val s = super@Outer.bar()
    }
}