tailRecursive fun withWhen2(counter : Int, x : Any) : Int =
        when {
            counter == 0 -> counter
            counter == 50 -> 1 + withWhen2(counter - 1, "no tail")
            withWhen2(0, "no tail") == 0 -> withWhen2(counter - 1, "tail")
            else -> 1
        }

fun box() : String = if (withWhen2(100000, "test") == 1) "OK" else "FAIL"