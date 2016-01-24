fun fn(c: Char?): Any? =
        if (c == null) TODO()
        else when (<!DEBUG_INFO_SMARTCAST!>c<!>) {
            'a' -> when (<!DEBUG_INFO_SMARTCAST!>c<!>) {
                'B' -> 1
                'C' -> "sdf"
                else -> TODO()
            }
            else -> TODO()
        }