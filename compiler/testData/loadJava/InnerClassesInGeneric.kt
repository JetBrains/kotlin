package test

public open class InnerClassesInGeneric<P, Q>() : java.lang.Object() {
    public open inner class Inner() : java.lang.Object() {
    }
    
    public open inner class Inner2() : Inner() {
        public open fun iterator() : jet.MutableIterator<P>? {
            throw UnsupportedOperationException()
        }
    }
}
