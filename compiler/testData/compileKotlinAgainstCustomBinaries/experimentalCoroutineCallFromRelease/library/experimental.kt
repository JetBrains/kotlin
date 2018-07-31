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

val c: suspend () -> Unit = {}

class WithTypeParameter<T: suspend() -> Unit> {}

fun returnsSuspend() : suspend() -> Unit = {}

fun builder(c: suspend () -> Unit) {}

fun <T: suspend () -> Unit> withTypeParameter() = {}