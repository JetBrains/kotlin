// RUN_PIPELINE_TILL: BACKEND
// LATEST_LV_DIFFERENCE
// IGNORE_DEXING
class Aaa() {
    <!CONFLICTING_JVM_DECLARATIONS!>val a<!> = 1
    <!CONFLICTING_JVM_DECLARATIONS!>@Deprecated("a", level = DeprecationLevel.HIDDEN)
    val a<!> = 1
}
