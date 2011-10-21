val {() : ()}.foo
val {foo.bar.() : ()}.foo

val {foo.bar.() : ()}.foo = foo
   get() {}
   set(it) {}

val {foo.bar.() : ()}.foo = foo
   get() : Foo {}
   set(it) {}


val {foo.bar.() : ()}.foo : bar = foo
   [a] public get() {}
   open set(a : b) {}


val {foo.bar.() : ()}.foo : bar = foo
   open set(a : b) {}


val {foo.bar.() : ()}.foo : bar = foo
   [a] public get() {}

// Error recovery:

val {foo.bar.() : ()}.foo = foo
   set) {}
   dfget() {}

val {foo.bar.() : ()}.foo = foo
   get(foo) {}
   set() {}
   set() {}

