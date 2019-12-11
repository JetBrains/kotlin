sealed class Sealed {
    object First: Sealed()
    open class NonFirst: Sealed() {
        object Second: NonFirst()
        object Third: NonFirst()
        fun foo(): Int {
            val s = object: Sealed() {}
            class Local: Sealed() {}
            return s.hashCode()
        }
    }
    val p: Sealed = object: Sealed() {}
}
