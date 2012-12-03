trait Trait {
    fun bar() = 42
}

class Outer : Trait {
    class Nested {
        val t = this<!INACCESSIBLE_OUTER_CLASS_EXPRESSION!>@Outer<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>bar<!>()
        val s = super<!INACCESSIBLE_OUTER_CLASS_EXPRESSION!>@Outer<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>bar<!>()
        
        inner class NestedInner {
            val t = this<!INACCESSIBLE_OUTER_CLASS_EXPRESSION!>@Outer<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>bar<!>()
            val s = super<!INACCESSIBLE_OUTER_CLASS_EXPRESSION!>@Outer<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>bar<!>()
        }
    }
    
    inner class Inner {
        val t = this@Outer.bar()
        val s = super@Outer.bar()
    }
}
