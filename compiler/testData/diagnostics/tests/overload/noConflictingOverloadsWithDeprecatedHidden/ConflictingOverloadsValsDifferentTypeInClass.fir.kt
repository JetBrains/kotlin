// RUN_PIPELINE_TILL: BACKEND
class Aaa() {
    val a = 1
    @Deprecated("a", level = DeprecationLevel.HIDDEN)
    val a = ""
}