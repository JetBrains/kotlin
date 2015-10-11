class Delegate {
    fun getValue(_this: Nothing?, p: PropertyMetadata): Int = 0
}

val a = Delegate()

val b by a