val test: Int = listOf<Any>().map {
    when (it) {
        is Int -> <!DEBUG_INFO_SMARTCAST!>it<!>
        else -> throw AssertionError()
    }
}.sum()