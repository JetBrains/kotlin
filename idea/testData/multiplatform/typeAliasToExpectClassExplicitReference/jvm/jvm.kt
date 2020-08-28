actual class A {
    actual fun commonMember() { }

    fun platformMember() { }
}

fun test() {
    <!RESOLUTION_TO_CLASSIFIER!>TypealiasFromCommon<!>().<!DEBUG_INFO_MISSING_UNRESOLVED!>commonMember<!>()
    <!RESOLUTION_TO_CLASSIFIER!>TypealiasFromCommon<!>().<!DEBUG_INFO_MISSING_UNRESOLVED!>platformwMember<!>()
}