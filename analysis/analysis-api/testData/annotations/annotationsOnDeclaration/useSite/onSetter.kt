annotation class A(val a: Int, val c: KClass<*>)

@set:A(1, Int::class)
var foo : Int
    se<caret>t(value) {
        field = value
    }