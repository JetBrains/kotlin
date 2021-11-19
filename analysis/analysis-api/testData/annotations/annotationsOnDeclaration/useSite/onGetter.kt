annotation class A(val a: Int, val c: KClass<*>)

@get:A(1, Int::class)
val foo : Int
    ge<caret>t() = 10