// IGNORE_BACKEND_K1: ANY
// ^^^ Context parameters aren't going to be supported in K1.
// MODULE: value_parameters_with_context_parameters_library

package value_parameters_with_context_parameters.test

fun funWithDefaultArgs(p1: Int = 42, p2: Long, p3: String = ""): String = ""

// This is needed to check that value parameter indices are properly deserialized for functions with context receivers.
context(Int, Long) fun funWithDefaultArgs(p1: Int = 42, p2: Long, p3: String = ""): String = ""
