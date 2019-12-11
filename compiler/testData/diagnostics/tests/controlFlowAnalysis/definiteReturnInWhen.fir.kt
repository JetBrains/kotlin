fun illegalWhenBlock(a: Any): Int {
    when(a) {
        is Int -> return a
        is String -> return a.length
    }
}
