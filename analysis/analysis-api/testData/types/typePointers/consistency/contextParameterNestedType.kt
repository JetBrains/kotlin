class Foo
interface Bar
interface Baz

fun test(foo: Foo, bar: Bar, block: <expr>(context(Foo) (String) -> context(Bar) (Int) -> Int) -> context(Baz) (Long) -> String</expr>) {}