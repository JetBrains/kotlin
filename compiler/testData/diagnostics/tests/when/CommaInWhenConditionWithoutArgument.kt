fun foo(x: Int, y: Int): Int =
        when {
            x > 0<!COMMA_IN_WHEN_CONDITION_WITHOUT_ARGUMENT!>,<!> y > 0<!COMMA_IN_WHEN_CONDITION_WITHOUT_ARGUMENT!>,<!><!SYNTAX!>,<!> x < 0 -> 1
            else -> 0
        }

fun bar(x: Int): Int =
        when (x) {
            0 -> 0
            else -> 1
        }