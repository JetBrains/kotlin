fun some() {
    when {
        true && true ->
        else
        <caret>-> Unit
    }
}

// SET_TRUE: ALIGN_IN_COLUMNS_CASE_BRANCH