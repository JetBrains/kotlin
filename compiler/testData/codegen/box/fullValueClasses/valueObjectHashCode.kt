// LANGUAGE: +FullValueClasses
// WITH_STDLIB

value object Foo

object Container {
    value object Nested
}

fun box(): String {
    if (Foo.hashCode() == 0) return "Fail: direct hashCode"
    val boxed: Any = Foo
    if (boxed.hashCode() != Foo.hashCode()) return "Fail: boxed hashCode"
    if (Container.Nested.hashCode() == 0) return "Fail: nested hashCode"
    val boxedNested: Any = Container.Nested
    if (boxedNested.hashCode() != Container.Nested.hashCode()) return "Fail: boxed nested hashCode"
    if (Foo.hashCode() == Container.Nested.hashCode()) return "Fail: identical object hash codes"
    return "OK"
}
