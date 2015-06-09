val ((Unit) -> Unit).foo
val (foo.bar.() -> Unit).foo

val (foo.bar.() -> Unit).foo = foo
   get() {}
   set(it) {}

val (foo.bar.() -> Unit).foo = foo
   get() : Foo {}
   set(it) {}


val (foo.bar.() -> Unit).foo : bar = foo
   @[a] public get() {}
   open set(a : b) {}


val (foo.bar.() -> Unit).foo : bar = foo
   open set(a : b) {}


val (foo.bar.() -> Unit).foo : bar = foo
   @[a] public get() {}

// Error recovery:

val (foo.bar.() -> Unit).foo = foo
   set) {}
   dfget() {}

val (foo.bar.() -> Unit).foo = foo
   get(foo) {}
   set() {}
   set() {}

