import kotlin.reflect.KClass

annotation class A(val a: Int, val c: KClass<*>)

@set:A(1, Int::class)
var foo: Int = 0
    se<caret>t(value) {
        field = value
    }