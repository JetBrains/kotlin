fun foo()
fun @[a] foo()
fun @[a] T.foo()
fun @[a] T.foo(a : foo) : bar
fun @[a()] T.foo<T :  (a) -> b>(a : foo) : bar

fun foo();
fun @[a] foo();
fun @[a] T.foo();
fun @[a] T.foo(a : foo) : bar;
fun @[a()] T.foo<T :  (a) -> b>(a : foo) : bar;

fun foo() {}
fun @[a] foo() {}
fun @[a] T.foo() {}
fun @[a] T.foo(a : foo) : bar {}
fun @[a()] T.foo<T :  (a) -> b>(a : foo) : bar {}

fun @[a()] T.foo<T : @[a]  (a) -> b>(a : foo) : bar {}

fun A?.foo() : bar?
fun A? .foo() : bar?
fun foo() = 5