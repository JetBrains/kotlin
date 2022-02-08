import My.bar
import My.baz
import My.gau
import My.wat
import My.watwat

fun <T> T.foo() {}

interface Your<R> {
    fun wat() {}
    fun <T> T.watwat() {}
}

object My : Your<Double> {
    fun <T> T.bar() {}
    <!NON_ABSTRACT_FUNCTION_WITH_NO_BODY!>fun baz()<!>
    fun Boolean.gau() {}
}

fun test() {
    42.foo()
    "".foo()

    42.bar()
    "".bar()

    baz()
    true.gau()
    wat()
    false.watwat()
}
