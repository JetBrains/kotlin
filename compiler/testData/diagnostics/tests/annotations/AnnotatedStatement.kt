// ISSUE: KT-67014, KT-67254
// WITH_STDLIB
// FILE: JavaAnn.java

public @interface JavaAnn {}

// FILE: test.kt
annotation class KotlinAnn

fun foo(list: MutableList<Int>, arr: Array<String>) {
    @JavaAnn @KotlinAnn
    when { else -> {} }

    @JavaAnn @KotlinAnn
    while (true) { break }

    @JavaAnn @KotlinAnn
    if (true) {}

    var x = <!VARIABLE_WITH_REDUNDANT_INITIALIZER!>1<!>

    @JavaAnn @KotlinAnn
    x = 2

    @JavaAnn @KotlinAnn
    x += 2

    @JavaAnn @KotlinAnn
    list += 2

    @JavaAnn @KotlinAnn
    arr[0] = ""

    @JavaAnn @KotlinAnn
    arr[1] += "*"
}
