open class Base<T> {
    open class Nested<K> // K

    open inner class Inner<U> // T, U
}

class Derived : Base<String>() {
    class DerivedNested : Nested<Int>() // Base.Nested<Int>
    // for name Nested
    //
    // symbol: Base.Nested<K>
    // subtitution from scope: {}
    // subtitution from arguments: K -> Int
    // result type: Base.Nested<Int>


    inner class DerivedInner : Inner<Long>() // Base<String>.Inner<Long>
    // for name Inner
    //
    // symbol: Base<T>.Inner<U>
    // subtitution from scope: T -> String
    // subtitution from arguments: U -> Long
    // result type: Base<String>.Inner<Long>
}
