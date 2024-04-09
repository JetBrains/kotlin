// ISSUE: KT-67014
// WITH_STDLIB
// FILE: JavaAnn.java

public @interface JavaAnn {}

// FILE: test.kt
annotation class KotlinAnn

fun foo(list: MutableList<Int>) {
    @JavaAnn @KotlinAnn
    when { else -> {} }

    @JavaAnn @KotlinAnn
    while (true) { break }

    @JavaAnn @KotlinAnn
    if (true) {}

    var x = 1

    @JavaAnn @KotlinAnn
    x += 2

    @JavaAnn @KotlinAnn
    list += 2
}
