annotation class A(val a: Int, val c: KClass<*>)

@A(1, Int::class)
val f<caret>oo: Int = 10