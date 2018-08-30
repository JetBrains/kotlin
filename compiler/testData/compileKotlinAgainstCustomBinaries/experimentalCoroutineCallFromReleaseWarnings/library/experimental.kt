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

fun builder(c: suspend () -> Unit) {}
fun builder2(c: suspend Int.(String) -> Unit) {}

fun (suspend (Int) -> Unit).start() {}

suspend fun suspendAcceptsSuspend(x: suspend () -> Unit) {}
