// KT-1188

trait Foo {
    val b : Bar
    val b1 : Foo.Bar
    trait Bar {}
}

trait Foo2 : Foo {
    val bb1 : Foo.Bar
}
