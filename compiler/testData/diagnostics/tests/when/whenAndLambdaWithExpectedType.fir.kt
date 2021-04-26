// !WITH_NEW_INFERENCE
/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-152
 * PRIMARY LINKS: expressions, when-expression -> paragraph 2 -> sentence 1
 * expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 1
 * type-system, subtyping, subtyping-rules -> paragraph 2 -> sentence 1
 * type-inference, local-type-inference -> paragraph 2 -> sentence 1
 * overload-resolution, determining-function-applicability-for-a-specific-call, description -> paragraph 3 -> sentence 3
 */

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
            true -> { s1, <!CANNOT_INFER_PARAMETER_TYPE!>s2<!> -> true }
            else -> null!!
        }

