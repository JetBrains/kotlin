annotation class A(val a: Int, val c: KClass<*>)

@A(1, Int::class)
fun fo<caret>o(): Int = 10