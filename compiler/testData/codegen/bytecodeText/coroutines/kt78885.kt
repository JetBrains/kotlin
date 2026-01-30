fun runBlocking(c: suspend () -> Unit) { TODO() }

suspend fun delay(int: Int) { TODO() }

fun println(a: Any) { TODO() }

fun main() = runBlocking {
    myInlineBlock {
        delay(1)
        val c = "c"
        println(c)
    }
}

private inline fun myInlineBlock(block: () -> Unit) = block()

// Make sure, that $i$f variable comes _before_ $i$a variable in the code range for label=1
// 1 LOCALVARIABLE \$i\$f\$myInlineBlock[\d\\]* I L10
// 1 LOCALVARIABLE \$i\$a\$-myInlineBlock-Kt78885Kt\$main\$1\$1[\d\\]* I L11

// We have no variable to spill/unspill
// 1 PUTFIELD .*label
// 0 ILOAD
