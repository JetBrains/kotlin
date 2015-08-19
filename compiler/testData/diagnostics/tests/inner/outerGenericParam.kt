class Outer<T> {
    class Nested {
        fun foo(t: <!UNRESOLVED_REFERENCE!>T<!>) = <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>t<!>
    }
    
    class Nested2<T> {
        fun foo(t: T) = t
    }
    
    inner class Inner {
        fun foo(t: T) = t
    }
}