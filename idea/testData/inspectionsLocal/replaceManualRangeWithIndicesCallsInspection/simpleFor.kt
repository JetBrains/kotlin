// WITH_RUNTIME
fun test(args: Array<String>) {
    for (index in 0..<caret>args.size-1) {
        val out = args[index]
    }
}
