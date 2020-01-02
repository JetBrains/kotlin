fun fn(c: Char?): Any? =
        if (c == null) TODO()
        else when (c) {
            'a' -> when (c) {
                'B' -> 1
                'C' -> "sdf"
                else -> TODO()
            }
            else -> TODO()
        }