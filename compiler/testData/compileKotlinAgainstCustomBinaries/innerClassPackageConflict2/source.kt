import test.Foo.*

// Note that unlike in Java, in Kotlin we currently mostly prefer package to class in qualified name resolution.
// So here, for example, we see both the package and the class with the name test.Foo, and prefer the former.
// So 'Bar' should be resolved, 'Nested' should be unresolved.
// For javac, the opposite is true: 'Bar' would be unresolved in a similar situation, 'Nested' would be resolved.

val v1: Bar? = null
val v2: test.Foo.Bar? = null
val v3: Nested? = null
val v4: test.Foo.Nested? = null

val v5: test.Boo.SubBoo.C.Nested? = null
val v6: test.Boo.Nested? = null
