// Issue: KT-31734

fun foo() {
    val x = { @Foo (foo, bar) -> }
    val x = { @Foo (foo: kotlin.Any, bar) -> }
    val x = { @Foo (foo, bar: Any) -> }
    val x = { @Foo ((foo, bar: Any)) -> }
    val x = { @Foo () -> Unit }
}
