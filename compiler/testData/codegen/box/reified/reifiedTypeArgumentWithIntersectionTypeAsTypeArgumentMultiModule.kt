// IGNORE_BACKEND: JVM_IR
// ^^^ KT-79201: Runtime issue: java.lang.UnsupportedOperationException:
//               This function has a reified type parameter and thus can only be inlined at compilation time, not called directly.
// When KT-79201 is fixed, please remove this test and move `box/reified/reifiedTypeArgumentWithIntersectionTypeAsTypeArgument.kt` to `boxInline/reified/`,
//   adding couple of `FILE:` test directives, as in every test in `boxInline/`
// WITH_STDLIB

// See KT-37163

// MODULE: lib
import kotlin.reflect.typeOf

class In<in T>

interface A
interface B

// TODO check real effects to fix the behavior when we reach consensus
//  and to be sure that something is not dropped by optimizations.

@OptIn(kotlin.ExperimentalStdlibApi::class)
inline fun <reified K> select(x: K, y: K) {
    x is K
    x as K
    K::class
    typeOf<K>()
    Array<K>(1) { x }
}

// MODULE: main(lib)
fun test() {
    select(In<A>(), In<B>())
}

fun box(): String {
    test()
    return "OK"
}
