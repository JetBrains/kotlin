// RUN_PIPELINE_TILL: BACKEND
// LATEST_LV_DIFFERENCE
// IGNORE_DEXING
// ISSUE: KT-22004

class A() {
    <!CONFLICTING_JVM_DECLARATIONS!>fun b() {
    }<!>

    <!CONFLICTING_JVM_DECLARATIONS!>@Deprecated("test", level = DeprecationLevel.HIDDEN)
    fun b() {
    }<!>
}
