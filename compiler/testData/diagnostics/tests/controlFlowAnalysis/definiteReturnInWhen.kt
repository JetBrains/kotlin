fun illegalWhenBlock(a: Any): Int {
    when(a) {
        is Int -> return <!DEBUG_INFO_SMARTCAST!>a<!>
        is String -> return <!DEBUG_INFO_SMARTCAST!>a<!>.length
    }
<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
