import test.Foo.Bar
import test.Baz

val f: Bar? = null
val g: Baz? = Baz().apply { bar() }
