suspend fun dummy() {}

class C {
    suspend fun dummy() = "OK"
}

class WithNested {
    class Nested {
        suspend fun dummy() = "OK"
    }
}

class WithInner {
    inner class Inner {
        suspend fun dummy() = "OK"
    }
}
