tailrec fun countDown(x: Int): Int = if (x == 0) 0 else countDown(x - 1)

external fun nativeCall()

internal fun internalFunction() {}

private fun privateFunction() {}
