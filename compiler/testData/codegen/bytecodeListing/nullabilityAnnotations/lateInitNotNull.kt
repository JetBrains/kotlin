// This test checks that we mark the backing field of a lateinit property with `@NotNull`.
class A {
    lateinit var x: A
}
