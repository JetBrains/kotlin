val test: Int = listOf<Any>().map {
    when (it) {
        is Int -> it
        else -> throw AssertionError()
    }
}.sum()