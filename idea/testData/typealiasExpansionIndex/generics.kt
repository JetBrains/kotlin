class A<T>

typealias TA<T> = A<T>
// CONTAINS (key="A", value="TA")

typealias TB = A<Any>
// CONTAINS (key="A", value="TB")