// RUN_PIPELINE_TILL: KLIB
class Aaa() {
    val a = 1
    @Deprecated("a", level = DeprecationLevel.HIDDEN)
    val a = 1
}