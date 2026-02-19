@Suppress("UNUSED_VARIABLE")
fun check() {
    val o = CustomSupplier<String> {
        3
    }

    val o2 = object : CustomSupplier<String> {}

    val o3 = object : CustomSupplier<String> {
        override fun get(): String = 2
    }
}

fun interface CustomSupplier<T> {
    fun get(): T
}
