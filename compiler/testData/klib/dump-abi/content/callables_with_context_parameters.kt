// IGNORE_BACKEND_K1: ANY
// ^^^ Context parameters aren't going to be supported in K1.
// MODULE: callables_with_context_parameters

package callables_with_context_parameters.test

context(Int) fun regularFun(): String = ""
context(Int) fun regularFun(p1: Number): String = ""
context(Int) fun regularFun(p1: Int): String = ""
context(Int) fun regularFun(p1: Int, p2: Long): String = ""
context(Int) fun regularFun(p1: Number, p2: Long): String = ""
context(Int) fun regularFun(p1: Int, p2: Number): String = ""
context(Int) fun regularFun(p1: Number, p2: Number): String = ""
context(Int) fun Int.regularFun(): String = ""
context(Int) fun Long.regularFun(): String = ""
context(Int) fun Number.regularFun(): String = ""

context(Int, Long) fun regularFun(): String = ""
context(Int, Long) fun regularFun(p1: Number): String = ""
context(Int, Long) fun regularFun(p1: Int): String = ""
context(Int, Long) fun regularFun(p1: Int, p2: Long): String = ""
context(Int, Long) fun regularFun(p1: Number, p2: Long): String = ""
context(Int, Long) fun regularFun(p1: Int, p2: Number): String = ""
context(Int, Long) fun regularFun(p1: Number, p2: Number): String = ""
context(Int, Long) fun Int.regularFun(): String = ""
context(Int, Long) fun Long.regularFun(): String = ""
context(Int, Long) fun Number.regularFun(): String = ""
context(Int, Long) fun funWithDefaultArgs(p1: Int = 42, p2: Long, p3: String = ""): String = ""

class FunctionContainer {
    context(Int) fun regularFun(): String = ""
    context(Int) fun regularFun(p1: Number): String = ""
    context(Int) fun regularFun(p1: Int): String = ""
    context(Int) fun regularFun(p1: Int, p2: Long): String = ""
    context(Int) fun regularFun(p1: Number, p2: Long): String = ""
    context(Int) fun regularFun(p1: Int, p2: Number): String = ""
    context(Int) fun regularFun(p1: Number, p2: Number): String = ""
    context(Int) fun Int.regularFun(): String = ""
    context(Int) fun Long.regularFun(): String = ""
    context(Int) fun Number.regularFun(): String = ""

    context(Int, Long) fun regularFun(): String = ""
    context(Int, Long) fun regularFun(p1: Number): String = ""
    context(Int, Long) fun regularFun(p1: Int): String = ""
    context(Int, Long) fun regularFun(p1: Int, p2: Long): String = ""
    context(Int, Long) fun regularFun(p1: Number, p2: Long): String = ""
    context(Int, Long) fun regularFun(p1: Int, p2: Number): String = ""
    context(Int, Long) fun regularFun(p1: Number, p2: Number): String = ""
    context(Int, Long) fun Int.regularFun(): String = ""
    context(Int, Long) fun Long.regularFun(): String = ""
    context(Int, Long) fun Number.regularFun(): String = ""
    context(Int, Long) fun funWithDefaultArgs(p1: Int = 42, p2: Long, p3: String = ""): String = ""
}
