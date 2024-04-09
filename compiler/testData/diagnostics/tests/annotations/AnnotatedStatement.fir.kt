// ISSUE: KT-67014
// WITH_STDLIB
// FILE: JavaAnn.java

public @interface JavaAnn {}

// FILE: test.kt
annotation class KotlinAnn

fun foo(list: MutableList<Int>) {
    <!WRONG_ANNOTATION_TARGET!>@JavaAnn<!> <!WRONG_ANNOTATION_TARGET!>@KotlinAnn<!>
    when { else -> {} }

    @JavaAnn @KotlinAnn
    while (true) { break }

    <!WRONG_ANNOTATION_TARGET!>@JavaAnn<!> <!WRONG_ANNOTATION_TARGET!>@KotlinAnn<!>
    if (true) {}

    var x = 1

    <!WRONG_ANNOTATION_TARGET!>@JavaAnn<!> <!WRONG_ANNOTATION_TARGET!>@KotlinAnn<!>
    x += 2

    <!WRONG_ANNOTATION_TARGET!>@JavaAnn<!> <!WRONG_ANNOTATION_TARGET!>@KotlinAnn<!>
    list += 2
}
