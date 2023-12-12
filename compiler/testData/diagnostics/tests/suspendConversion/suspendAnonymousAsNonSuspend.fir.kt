// ISSUE: KT-58055

fun <T> produce(arg: () -> T): T = arg()

fun main() {
    produce {
        <!ARGUMENT_TYPE_MISMATCH!><!ANONYMOUS_SUSPEND_FUNCTION!>suspend<!> fun() {}<!> // CCE
    }
}
