// p.C
// !GENERATE_PROPERTY_ANNOTATIONS_METHODS
package p

annotation class Anno

interface A {
    @Anno
    var prop1: Int
        get() = 0
        set(value) {}

    fun a() = "a"
}

interface B: A {
    @Anno
    var prop2: Int
        get() = 0
        set(value) {}

    fun b() = "b"
}

interface C<T> : B {
    fun c() = "c"

    @Anno
    var prop3: Int
        get() = 0
        set(value) {}

    fun more(): String
}
