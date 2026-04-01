// WITH_STDLIB
// IGNORE_FE10

// MODULE: main
// FILE: main.kt

fun foo() {
    val a = StringBuilder()
    a.append("String")
    a.append(10)
    a.append(0.0)
}

fun bar() = buildString {
    append("String")
    append(10)
    append(0.0)
}

fun test() = buildString {
    val a = CharArray(25)
    for (index in 0 until 24) {
        a[index] = 'a' + index
    }
    append(a)
}