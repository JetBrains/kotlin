class Outer {
    open class OpenNested
    class FinalNested
    
    open inner class OpenInner
    class FinalInner

    class Nested1 : OpenNested()
    class Nested2 : FinalNested()
    class Nested3 : OpenInner()
    class Nested4 : FinalInner()

    inner class Inner1 : OpenNested()
    inner class Inner2 : FinalNested()
    inner class Inner3 : OpenInner()
    inner class Inner4 : FinalInner()
}