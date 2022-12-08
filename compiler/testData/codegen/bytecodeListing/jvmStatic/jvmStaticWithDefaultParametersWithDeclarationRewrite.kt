// WITH_STDLIB
// REWRITE_JVM_STATIC_IN_COMPANION

class WithCompanion {
    companion object {
        @JvmStatic
        fun foo(x: Int = 1) {}
    }
}

object AnObject {
    @JvmStatic
    fun foo(x: Int = 1) {}
}