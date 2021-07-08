fun illegalWhenBlock(a: Any): Int {
    when(a) {
        is Int -> return a
        is String -> return a.length
    }
<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
