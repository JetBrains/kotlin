@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
fun removedFunName(): String = Baz::foo.name

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
fun removedFunString(): String = Baz::foo.toString()

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
fun removedFunCall(): Int = Baz().foo()