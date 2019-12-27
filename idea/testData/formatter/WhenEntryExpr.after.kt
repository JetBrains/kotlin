fun some(x: Any) {
    when (x) {
        is Int ->
            0
        3 ->
            2
        in 0..3 ->
            2
        else ->
            1
    }
    when (x) {
        is Int -> {
            0
        }
        3 -> {
            2
        }
        in 0..3 -> {
            2
        }
        else -> {
            1
        }
    }
    when (x) {
        is Int -> {
            0
        }
        3 -> {
            2
        }
        in 0..3 -> {
            2
        }
        else -> {
            1
        }
    }
    when (x) {
        is
        Int,
        -> {
            0
        }
        3,
        -> {
            2
        }
        in
        0..3,
        -> {
            2
        }
        else
        -> {
            1
        }
    }
}
