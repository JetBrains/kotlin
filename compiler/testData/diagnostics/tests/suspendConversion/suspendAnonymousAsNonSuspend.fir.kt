// ISSUE: KT-58055

fun <T> produce(arg: () -> T): T = arg()

fun main() {
    <!CANNOT_INFER_PARAMETER_TYPE!>produce<!> {
        <!ARGUMENT_TYPE_MISMATCH!><!ANONYMOUS_SUSPEND_FUNCTION!>suspend<!> fun() {}<!> // CCE
    }
}
