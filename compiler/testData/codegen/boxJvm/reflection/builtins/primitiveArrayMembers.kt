// TARGET_BACKEND: JVM
// WITH_REFLECT

// This test calls `members` on primitive arrays to make sure that it doesn't crash. Note that accessing some information from
// those members still crashes (and crashed in the K1 implementation), which is why primitive arrays are ignored in
// `ReflectionIntegrationTest.testBuiltinClasses`. Once those crashes are fixed, they can be unignored, and this test can be removed.

fun box(): String {
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
