external enum Foo { A, B }

fun box(a: Any, b: Any): Pair<Foo, Foo?> {
    return Pair(<!UNCHECKED_CAST_TO_EXTERNAL_ENUM!>a as Foo<!>, <!UNCHECKED_CAST_TO_EXTERNAL_ENUM!>b as? Foo<!>)
}