// IGNORE_BACKEND_K1: ANY
// ^^^ Context parameters aren't going to be supported in K1.
// LANGUAGE: +ContextParameters
// MODULE: callables_with_context_parameters

package callables_with_context_parameters.test

context(c1: Int) fun regularFun(): String = ""
context(c1: Int) fun regularFun(p1: Number): String = ""
context(c1: Int) fun regularFun(p1: Int): String = ""
context(c1: Int) fun regularFun(p1: Int, p2: Long): String = ""
context(c1: Int) fun regularFun(p1: Number, p2: Long): String = ""
context(c1: Int) fun regularFun(p1: Int, p2: Number): String = ""
context(c1: Int) fun regularFun(p1: Number, p2: Number): String = ""
context(c1: Int) fun Int.regularFun(): String = ""
context(c1: Int) fun Long.regularFun(): String = ""
context(c1: Int) fun Number.regularFun(): String = ""

context(c1: Int, c2: Long) fun regularFun(): String = ""
context(c1: Int, c2: Long) fun regularFun(p1: Number): String = ""
context(c1: Int, c2: Long) fun regularFun(p1: Int): String = ""
context(c1: Int, c2: Long) fun regularFun(p1: Int, p2: Long): String = ""
context(c1: Int, c2: Long) fun regularFun(p1: Number, p2: Long): String = ""
context(c1: Int, c2: Long) fun regularFun(p1: Int, p2: Number): String = ""
context(c1: Int, c2: Long) fun regularFun(p1: Number, p2: Number): String = ""
context(c1: Int, c2: Long) fun Int.regularFun(): String = ""
context(c1: Int, c2: Long) fun Long.regularFun(): String = ""
context(c1: Int, c2: Long) fun Number.regularFun(): String = ""
context(c1: Int, c2: Long) fun funWithDefaultArgs(p1: Int = 42, p2: Long, p3: String = ""): String = ""

class FunctionContainer {
    context(c1: Int) fun regularFun(): String = ""
    context(c1: Int) fun regularFun(p1: Number): String = ""
    context(c1: Int) fun regularFun(p1: Int): String = ""
    context(c1: Int) fun regularFun(p1: Int, p2: Long): String = ""
    context(c1: Int) fun regularFun(p1: Number, p2: Long): String = ""
    context(c1: Int) fun regularFun(p1: Int, p2: Number): String = ""
    context(c1: Int) fun regularFun(p1: Number, p2: Number): String = ""
    context(c1: Int) fun Int.regularFun(): String = ""
    context(c1: Int) fun Long.regularFun(): String = ""
    context(c1: Int) fun Number.regularFun(): String = ""

    context(c1: Int, c2: Long) fun regularFun(): String = ""
    context(c1: Int, c2: Long) fun regularFun(p1: Number): String = ""
    context(c1: Int, c2: Long) fun regularFun(p1: Int): String = ""
    context(c1: Int, c2: Long) fun regularFun(p1: Int, p2: Long): String = ""
    context(c1: Int, c2: Long) fun regularFun(p1: Number, p2: Long): String = ""
    context(c1: Int, c2: Long) fun regularFun(p1: Int, p2: Number): String = ""
    context(c1: Int, c2: Long) fun regularFun(p1: Number, p2: Number): String = ""
    context(c1: Int, c2: Long) fun Int.regularFun(): String = ""
    context(c1: Int, c2: Long) fun Long.regularFun(): String = ""
    context(c1: Int, c2: Long) fun Number.regularFun(): String = ""
    context(c1: Int, c2: Long) fun funWithDefaultArgs(p1: Int = 42, p2: Long, p3: String = ""): String = ""
}
