package test

class CompositeIterator<T>(vararg iterators: java.util.Iterator<T>){
    val iteratorsIter = iterators.iterator()
}
