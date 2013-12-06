fun illegalWhenBlock(a: Any): Any {
    when(a) {
        is Int -> return a
    }
}