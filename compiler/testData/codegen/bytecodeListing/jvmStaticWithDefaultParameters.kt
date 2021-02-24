// WITH_RUNTIME

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