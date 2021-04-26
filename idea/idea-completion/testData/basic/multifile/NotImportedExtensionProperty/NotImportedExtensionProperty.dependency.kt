package abc

val String.helloProp1: Int get() = 1
val String.helloProp2: Int get() = 2
val Int.helloProp3: Int get() = 3
val helloProp4: Int = 4

private val String.helloPropPrivate get() = 1