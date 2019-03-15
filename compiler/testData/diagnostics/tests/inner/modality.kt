class Outer {
    open class OpenNested
    class FinalNested
    
    open inner class OpenInner
    class FinalInner

    class Nested1 : OpenNested()
    class Nested2 : <!FINAL_SUPERTYPE!>FinalNested<!>()
    class Nested3 : <!DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE!>OpenInner<!>()
    class Nested4 : <!FINAL_SUPERTYPE!>FinalInner<!>()

    inner class Inner1 : OpenNested()
    inner class Inner2 : <!FINAL_SUPERTYPE!>FinalNested<!>()
    inner class Inner3 : OpenInner()
    inner class Inner4 : <!FINAL_SUPERTYPE!>FinalInner<!>()
}