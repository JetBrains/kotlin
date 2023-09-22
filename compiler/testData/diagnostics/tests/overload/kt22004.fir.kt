// ISSUE: KT-22004

class A() {
    fun b() {
    }

    @Deprecated("test", level = DeprecationLevel.HIDDEN)
    fun b() {
    }
}
