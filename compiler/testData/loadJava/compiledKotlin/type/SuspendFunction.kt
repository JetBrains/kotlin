package test

fun test1(): suspend () -> Unit = null!!
fun test2(): suspend (Int, String) -> Int = null!!
fun test3(): suspend Int.(String) -> Int = null!!
fun test4(): List<suspend () -> Unit> = null!!
fun test5(): suspend (suspend () -> Unit) -> Unit = null!!
