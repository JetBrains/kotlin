@file:Suppress("UNUSED_VARIABLE", "UNUSED_PARAMETER")

fun <T> runBlocking(block: suspend CoroutineScope.() -> T): T = TODO()

class CoroutineScope

fun test() {
    runBlocking {
        repeat(1) {
            java.lang.Thread.<warning descr="Inappropriate blocking method call">sleep</warning>(1)
        }
    }
}

suspend fun test2() {
    val unused1 = run { Thread.<warning descr="Inappropriate blocking method call">sleep</warning>(2) }

    val unused2 = run (fun() {Thread.<warning descr="Inappropriate blocking method call">sleep</warning>(3)})
}