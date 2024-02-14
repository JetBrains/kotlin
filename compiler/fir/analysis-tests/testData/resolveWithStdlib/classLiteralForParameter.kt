inline fun <reified T : Any> foo(t: T): T {
    val klass = T::class.java
    return t
}