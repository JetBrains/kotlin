// RUN_PIPELINE_TILL: SOURCE
package bar

fun main() {
    class Some

    Some[<!SYNTAX!><!>] names <!SYNTAX!>=<!> <!NO_GET_METHOD!>["ads"]<!>
}
