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
    @JavaAnnWithTarget @JavaAnn @KotlinAnn
    when { else -> {} }

    @JavaAnnWithTarget @JavaAnn @KotlinAnn
    while (true) { break }

    @JavaAnnWithTarget @JavaAnn @KotlinAnn
    for (i in list) {}

    @JavaAnnWithTarget @JavaAnn @KotlinAnn
    if (true) {}

    var x = <!VARIABLE_WITH_REDUNDANT_INITIALIZER!>1<!>

    @JavaAnnWithTarget @JavaAnn @KotlinAnn
    x = 2

    @JavaAnnWithTarget @JavaAnn @KotlinAnn
    x += 2

    @JavaAnnWithTarget @JavaAnn @KotlinAnn
    list += 2

    @JavaAnnWithTarget @JavaAnn @KotlinAnn
    arr[0] = ""

    @JavaAnnWithTarget @JavaAnn @KotlinAnn
    arr[1] += "*"

    <!WRONG_ANNOTATION_TARGET!>@JavaAnnWithTarget<!> @JavaAnn @KotlinAnn
    { bar() }
}

fun bar() {}
