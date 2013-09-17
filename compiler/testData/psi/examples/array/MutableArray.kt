/**
  These declarations are "shallow" in the sense that they are not really compiled, only the type-checker uses them
*/

open class ReadOnlyArray<out T> : ISized {
  [operator] fun get(index : Int) : T
}

open class WriteOnlyArray<in T> : ISized { // This is needed to keep IIterator's <T> covariant
  [operator] fun set(index : Int, value : T)
}

class MutableArray<T> : ReadOnlyArray<T>, WriteOnlyArray<T> {/*...*/}