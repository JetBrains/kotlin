val test1: (String) -> Boolean =
        when {
            true -> {{ true }}
            else -> {{ false }}
        }

val test2: (String) -> Boolean =
        when {
            true -> {{ true }}
            else -> null!!
        }

val test3: (String) -> Boolean =
        when {
            true -> { s -> true }
            else -> null!!
        }

val test4: (String) -> Boolean =
        when {
            true -> { <!EXPECTED_PARAMETERS_NUMBER_MISMATCH!>s1, <!CANNOT_INFER_PARAMETER_TYPE!>s2<!><!> -> true }
            else -> null!!
        }

