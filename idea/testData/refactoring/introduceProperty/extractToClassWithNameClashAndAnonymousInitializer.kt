// EXTRACTION_TARGET: property with initializer

class A {
    val i: Int

    {
        i = 1
    }

    fun foo(): Int {
        return <selection>1 + 2</selection>
    }
}

