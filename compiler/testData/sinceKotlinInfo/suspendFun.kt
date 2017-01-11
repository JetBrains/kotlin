package test

suspend fun topLevel() {}

class Foo {
    constructor(block: suspend () -> Unit)

    suspend fun member() {}
}

fun async1(block: suspend () -> Unit) {}
fun (suspend () -> Unit).async2() {}
fun async3(): suspend () -> Unit = null!!
fun async4(): Map<Int, suspend () -> Unit>? = null

val (suspend () -> Unit).asyncVal: () -> Unit get() = {}
