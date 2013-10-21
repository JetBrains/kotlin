tailRecursive fun withWhen(counter : Int, d : Any, x : Any) : Int =
        when (counter) {
            0 -> counter
            1, 2 -> withWhen(counter - 1, "1,2", "tail")
            in 3..49 -> withWhen(counter - 1, "3..49", "tail")
            50 -> 1 + withWhen(counter - 1, "50", "no tail")
            !in 0..50 -> withWhen(counter - 1, "!0..50", "tail")
            else -> withWhen(counter - 1, "else", "tail")
        }

fun box() : String = if (withWhen(100000, "test", "test") == 1) "OK" else "FAIL"