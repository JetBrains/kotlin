annotation class A(val a: Int, val c: KClass<*>)

@A(1, Int::class)
typealias F<caret>oo = String