annotation class A(val a: Int, val c: KClass<*>)

@property:A(1, Int::class)
val f<caret>oo : Int
    get() = 10