package test

class CompositeIterator<T>(vararg iterators: <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java.util.Iterator<T><!>){
    val iteratorsIter = iterators.iterator()
}