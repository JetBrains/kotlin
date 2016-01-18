fun TODO(): Nothing = null!!

fun fn(c: Char?): Any? =
        if (c == null) TODO()
        else when (<!DEBUG_INFO_SMARTCAST!>c<!>) {
            'a' -> when (<!DEBUG_INFO_SMARTCAST!>c<!>) {
                'B' -> <!IMPLICIT_CAST_TO_ANY!>1<!>
                'C' -> <!IMPLICIT_CAST_TO_ANY!>"sdf"<!>
                else -> TODO()
            }
            else -> TODO()
        }