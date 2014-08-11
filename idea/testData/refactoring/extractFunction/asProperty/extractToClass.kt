// EXTRACT_AS_PROPERTY

class A(val n: Int = 1) {
    val m: Int = 2
    // SIBLING:
    fun foo(): Int {
        return <selection>m + n + 1</selection>
    }
}

