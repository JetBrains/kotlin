package test

interface Call<T> {
    fun call(): T
}

public inline fun <reified T: Any> Any.inlineMeIfYouCan() : () -> Call<T> = {
    object : Call<T> {
        override fun call() = T::class.java.newInstance()
    }
}