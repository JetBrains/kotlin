//FILE: foo.kt
import bar
fun foo(x: Int): Int {
    if (x >= 0) {   // 4
        return x    // 5
    }
    return bar(x)   // 7
}

//FILE: test.kt
import foo
fun box() {
    foo(-3)            //4
}

fun bar(x: Int) =
    if (x < 0) {           //8
        foo(0)
    } else {               // 10
        foo(x)
    }

// IGNORE_BACKEND: JVM_IR

// IR backend has bar().12 replaced as bar().8 for returning bar() function result.
// LINENUMBERS
// TestKt.box():4
// FooKt.foo(int):4
// FooKt.foo(int):7
// TestKt.bar(int):8
// TestKt.bar(int):9
// FooKt.foo(int):4
// FooKt.foo(int):5
// TestKt.bar(int):9
// TestKt.bar(int):12
// FooKt.foo(int):7
// TestKt.box():4
// TestKt.box():5
