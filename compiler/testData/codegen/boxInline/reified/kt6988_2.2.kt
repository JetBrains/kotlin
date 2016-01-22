package test


public inline fun <reified T: Any> Any.inlineMeIfYouCan() : () -> Runnable = {
    object : Runnable {
        override fun run() {
            T::class.java.newInstance()
        }
    }
}