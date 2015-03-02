// WITH_RUNTIME
// EXTRACTION_TARGET: lazy property
class A {
    fun foo(): Int {
        <selection>val a = 1 + 2
        val b = a*2</selection>
        val c = b - 1
    }
}

