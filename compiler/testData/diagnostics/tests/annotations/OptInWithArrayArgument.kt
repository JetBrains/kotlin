// ISSUE: KT-65844

@RequiresOptIn
annotation class MyOptIn

@MyOptIn
fun foo() {}

@OptIn(markerClass = [<!OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN!>MyOptIn<!>::class]) // should be ok
class MyClass {
    fun test() {
        foo()
    }
}
