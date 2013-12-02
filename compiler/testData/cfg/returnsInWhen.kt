fun illegalWhenBlock(a: Any): Any {
    when(a) {
        is Int -> return a
        is String -> return a
    }
}