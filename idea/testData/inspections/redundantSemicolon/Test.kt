package p; // redundant

import java.util.ArrayList; // redundant

class A {
    fun foo() {
        print(1); print(2); // redundant
    }; // redundant

    fun bar() {}
}; // redundant

enum class E {
    A, B; // let's not consider it to be redundant
}

fun foo(p: Int) {
    if (p > 0) foo(); // not redundant!

    { p: Int ->
        print(p)
    }.doIt()
}

fun ((Int) -> Unit).doIt() {
    this.invoke(1); // redundant
}

fun bar() {
    a(); // redundant
    b()
}

fun baz(args: Array<String>) {
    for (arg in args);
    while (args.size > 0);
    // But here redundant!
    do while (args.size > 0);
}

enum class Foo {
    ; //not redundant
    ;
    companion object;
    ;
}

enum class Bar {
    ; //redundant
}