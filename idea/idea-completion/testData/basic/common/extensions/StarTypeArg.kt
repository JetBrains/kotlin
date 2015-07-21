class MyClass<T>

fun <T> MyClass<T>.ext() = ""

fun foo(t: MyClass<*>) {
    t.<caret>
}

// EXIST: ext
