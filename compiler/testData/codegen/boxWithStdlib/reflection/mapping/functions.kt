import kotlin.reflect.*
import kotlin.reflect.jvm.*

class K {
    fun foo(s: String): Int = s.length()
}
fun bar(s: String): Int = s.length()
fun String.baz(): Int = this.length()

fun check(f: KFunction<Int>) {
    assert(f.javaConstructor == null) { "Fail f constructor" }
    assert(f.javaMethod != null) { "Fail f method" }
    val m = f.javaMethod!!

    assert(m.kotlinFunction != null) { "Fail m function" }
    val ff = m.kotlinFunction!!

    assert(f == ff) { "Fail f != ff" }
}

fun box(): String {
    check(K::foo)
    check(::bar)
    check(String::baz)

    return "OK"
}
