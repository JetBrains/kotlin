// RUN_PIPELINE_TILL: FRONTEND
fun main(args: Array<String>) {
    return
        <!UNREACHABLE_CODE!>.
        <!ILLEGAL_SELECTOR, UNSIGNED_LITERAL_WITHOUT_DECLARATIONS_ON_CLASSPATH!>1u<!><!> // The expression cannot be a selector (occur after a dot)
    <!UNREACHABLE_CODE!>throw AssertionError()<!>
}
