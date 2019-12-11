class A : B() {
    open class Nested<T>
}

typealias ANested<T> = A.Nested<T>

open class B : ANested<Int>()