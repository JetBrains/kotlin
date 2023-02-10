// ISSUE: KT-17817

fun main(args: Array<String>) {
    <!UNRESOLVED_REFERENCE!>foo<!>().Wrapper<<!PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT!>*<!>>::class // no errors
}
