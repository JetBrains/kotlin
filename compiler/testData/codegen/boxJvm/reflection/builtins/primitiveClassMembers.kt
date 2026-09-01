// TARGET_BACKEND: JVM
// WITH_REFLECT

// This test calls `members` on primitive classes and arrays to make sure that it doesn't crash. Note that accessing some information from
// those members still crashes (and crashed in the K1 implementation), which is why primitive classes are ignored in
// `ReflectionIntegrationTest.testBuiltinClasses`. Once those crashes are fixed, they can be unignored, and this test can be removed.

fun box(): String {
    Boolean::class.members
    Char::class.members
    Byte::class.members
    Short::class.members
    Int::class.members
    Float::class.members
    Long::class.members
    Double::class.members
    BooleanArray::class.members
    CharArray::class.members
    ByteArray::class.members
    ShortArray::class.members
    IntArray::class.members
    FloatArray::class.members
    LongArray::class.members
    DoubleArray::class.members

    return "OK"
}
