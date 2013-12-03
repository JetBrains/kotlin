class Delegate {
    fun get(_this: Any, p: PropertyMetadata): Int = 0
}

val a = Delegate()

val b by a