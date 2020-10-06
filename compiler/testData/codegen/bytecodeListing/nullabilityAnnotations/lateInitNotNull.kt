// Test to ensure that we mark the backing field of a lateinit property
// as NotNull, even though the field is nullable in the JVM IR backend.
class A {
    lateinit var x: A
}
