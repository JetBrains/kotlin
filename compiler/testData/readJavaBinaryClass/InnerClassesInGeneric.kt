package test

public open class InnerClassesInGeneric<P, Q>() : java.lang.Object() {
    public open class Inner() : java.lang.Object() {
    }
    
    public open class Inner2() : Inner(), java.lang.Iterable<P> {
        override fun iterator() : java.util.Iterator<P>? {
            throw UnsupportedOperationException()
        }
    }
}
