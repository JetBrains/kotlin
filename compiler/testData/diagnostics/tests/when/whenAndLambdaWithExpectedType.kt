// !WITH_NEW_INFERENCE
val test1: (String) -> Boolean =
        when {
            true -> {{ true }}
            else -> {{ false }}
        }

val test2: (String) -> Boolean =
        when {
            true -> <!NI;TYPE_MISMATCH!>{{ true }}<!>
            else -> null!!
        }

val test3: (String) -> Boolean =
        when {
            true -> <!NI;TYPE_MISMATCH!>{ <!UNUSED_ANONYMOUS_PARAMETER!>s<!> -> true }<!>
            else -> null!!
        }

val test4: (String) -> Boolean =
        when {
            true -> <!NI;TYPE_MISMATCH!>{ <!OI;EXPECTED_PARAMETERS_NUMBER_MISMATCH!><!UNUSED_ANONYMOUS_PARAMETER!>s1<!>, <!OI;CANNOT_INFER_PARAMETER_TYPE, UNUSED_ANONYMOUS_PARAMETER!>s2<!><!> -> true }<!>
            else -> null!!
        }

