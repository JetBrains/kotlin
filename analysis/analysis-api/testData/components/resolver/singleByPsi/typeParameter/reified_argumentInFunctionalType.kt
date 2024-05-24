inline fun <reified T> functionWithLambda(t: T, process: (<caret>T) -> Int): Int = process(t)
