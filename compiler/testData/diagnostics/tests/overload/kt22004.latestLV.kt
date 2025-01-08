// RUN_PIPELINE_TILL: BACKEND
// LATEST_LV_DIFFERENCE
// IGNORE_DEXING
// ISSUE: KT-22004

class A() {
    fun b() {
    }

    @Deprecated("test", level = DeprecationLevel.HIDDEN)
    fun b() {
    }
}
