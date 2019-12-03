// IGNORE_FIR

class Generic<T>

class C {
    val prop: Generic<<caret>Foo>.
}

class Foo

// REF: (<root>).Foo
