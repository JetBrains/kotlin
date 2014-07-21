package test

public open class InnerClassesInGeneric<P, Q>() {
    public open inner class Inner() {
    }
    
    public open inner class Inner2() : Inner() {
        public open fun iterator() : kotlin.MutableIterator<P>? {
            throw UnsupportedOperationException()
        }
    }
}
