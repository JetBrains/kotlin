fun some(x: Any) {
    when (x) {
        is Number -> 0
        else -> 1
    }
}

// SET_FALSE: ALIGN_IN_COLUMNS_CASE_BRANCH