inline operator fun <reified T> Int.invoke() = this

val a2 = 1()
val a3 = 1.invoke()
