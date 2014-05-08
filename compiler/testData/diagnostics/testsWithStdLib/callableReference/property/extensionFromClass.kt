class A {
    fun test() {
        ::foo : KExtensionProperty<A, String>
        ::bar : KMutableExtensionProperty<A, Int>
    }
}

val A.foo: String get() = ""
var A.bar: Int
    get() = 42
    set(value) { }
