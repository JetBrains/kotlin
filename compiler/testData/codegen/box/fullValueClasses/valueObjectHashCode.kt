// LANGUAGE: +FullValueClasses
// WITH_STDLIB

value object Foo

object Container {
    value object Nested
}

fun box(): String {
    if (Foo.hashCode() != 0) return "Fail: direct hashCode"
    val boxed: Any = Foo
    if (boxed.hashCode() != 0) return "Fail: boxed hashCode"
    if (Container.Nested.hashCode() != 0) return "Fail: nested hashCode"
    val boxedNested: Any = Container.Nested
    if (boxedNested.hashCode() != 0) return "Fail: boxed nested hashCode"
    return "OK"
}
