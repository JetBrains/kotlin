// !WITH_NEW_INFERENCE
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
            true -> { s1, s2 -> true }
            else -> null!!
        }

