inline fun <reified T> functionWithLambda(t: <caret>T, process: (T) -> Int): Int = process(t)
