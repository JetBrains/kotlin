// ISSUE: KT-67014, KT-67254, KT-60234
// WITH_STDLIB
// FILE: JavaAnn.java

public @interface JavaAnn {}

// FILE: JavaAnnWithTarget.java

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
public @interface JavaAnnWithTarget {}

// FILE: test.kt
annotation class KotlinAnn

fun foo(list: MutableList<Int>, arr: Array<String>) {
    @JavaAnnWithTarget @JavaAnn <!WRONG_ANNOTATION_TARGET!>@KotlinAnn<!>
    when { else -> {} }

    @JavaAnnWithTarget @JavaAnn <!WRONG_ANNOTATION_TARGET!>@KotlinAnn<!>
    while (true) { break }

    @JavaAnnWithTarget @JavaAnn <!WRONG_ANNOTATION_TARGET!>@KotlinAnn<!>
    for (i in list) {}

    @JavaAnnWithTarget @JavaAnn <!WRONG_ANNOTATION_TARGET!>@KotlinAnn<!>
    if (true) {}

    var x = 1

    @JavaAnnWithTarget @JavaAnn <!WRONG_ANNOTATION_TARGET!>@KotlinAnn<!>
    x = 2

    @JavaAnnWithTarget @JavaAnn <!WRONG_ANNOTATION_TARGET!>@KotlinAnn<!>
    x += 2

    @JavaAnnWithTarget @JavaAnn <!WRONG_ANNOTATION_TARGET!>@KotlinAnn<!>
    list += 2

    @JavaAnnWithTarget @JavaAnn <!WRONG_ANNOTATION_TARGET!>@KotlinAnn<!>
    arr[0] = ""

    @JavaAnnWithTarget @JavaAnn <!WRONG_ANNOTATION_TARGET!>@KotlinAnn<!>
    arr[1] += "*"

    @JavaAnnWithTarget @JavaAnn @KotlinAnn
    { bar() }
}

fun bar() {}
