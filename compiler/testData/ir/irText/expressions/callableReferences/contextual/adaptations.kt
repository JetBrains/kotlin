// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// WITH_STDLIB
import kotlin.coroutines.RestrictsSuspension

context(s: String)
fun foo(p: Int = 1) {}

context(s: String)
fun bar(vararg p: Int) {}

@RestrictsSuspension
class Foo {
    context(_: String)
    suspend fun foo() {}
}

context(_: String)
suspend fun Foo.baz() {}

context(_: Foo)
suspend fun baz() {}

context(_: Foo)
fun String.test() {
    val suspendConversion: suspend (Int) -> Unit = ::foo
    val default: () -> Unit = ::foo
    val vararg0: () -> Unit = ::bar
    val vararg1: (Int) -> Unit = ::bar
    val vararg2: (Int, Int) -> Unit = ::bar

    val restricted1 = Foo::foo
    val restricted2 = Foo::baz
    val restricted3 = ::baz
}
