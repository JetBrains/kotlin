class Outer<T> {
    class Nested {
        fun foo(t: <!UNRESOLVED_REFERENCE!>T<!>) = t
    }
    
    class Nested2<T> {
        fun foo(t: T) = t
    }
    
    inner class Inner {
        fun foo(t: T) = t
    }
}
