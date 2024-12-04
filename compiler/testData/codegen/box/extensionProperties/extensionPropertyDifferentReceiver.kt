val Unit.foo: Boolean
    get() = true

val Nothing.bar: Boolean
    get() = true

class A
val A?.baz: Boolean
    get() = true

fun box(): String {
    return if ({ }().foo && (null?.bar ==null) && null.baz) "OK"
    else "FAIL"
}