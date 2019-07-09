interface Box<out R>
fun <R> List<Box<R>>.choose(): Box<R>? = TODO()
fun list(): List<Box<*>> = TODO()

fun f() = list().choose()
