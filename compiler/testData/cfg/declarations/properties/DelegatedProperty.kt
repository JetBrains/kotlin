class Delegate {
    fun get(_this: Nothing?, p: PropertyMetadata): Int = 0
}

val a = Delegate()

val b by a