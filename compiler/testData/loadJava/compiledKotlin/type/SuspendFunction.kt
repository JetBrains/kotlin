//ALLOW_AST_ACCESS

package test

fun test1(): suspend () -> Unit = null!!
fun test1N(): (suspend () -> Unit)? = null
fun test2(): suspend Int.() -> Int = null!!
fun test2N(): (suspend Int.() -> Int)? = null
fun test3(): List<suspend () -> Unit> = null!!
fun test3N(): List<(suspend () -> Unit)?> = null!!
fun test4(): suspend () -> (suspend () -> Unit) = null!!
fun test4N(): (suspend () -> (suspend () -> Unit)?)? = null
