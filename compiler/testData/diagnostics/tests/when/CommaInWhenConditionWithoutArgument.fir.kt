fun foo(x: Int, y: Int): Int =
        when {
            x > 0, y > 0,<!SYNTAX!>,<!> x < 0 -> 1
            else -> 0
        }

fun bar(x: Int): Int =
        when (x) {
            0 -> 0
            else -> 1
        }