// WITH_REFLECT
// FILE: FromJava.java

import kotlin.reflect.KCallable;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import kotlin.jvm.functions.Function3;

public class FromJava {
    public static String test(KCallable<?> x) {
        if (!(x instanceof Function1)) return "Fail 6";
        if (!(x instanceof Function2)) return "Fail 7";
        if (!(x instanceof Function3)) return "Fail 8";
        return "OK";
    }
}

// FILE: test.kt

class Foo {
    fun bar(x: Int): Int = x + 1
}

fun box(): String {
    val bar = Foo::class.members.single { it.name == "bar" }

    if (bar is Function1<*, *>) return "Fail 1"
    if (bar !is Function2<*, *, *>) return "Fail 2"
    if (bar is Function3<*, *, *, *>) return "Fail 3"

    bar as? Function2<Foo, Int, Int> ?: return "Fail 4"

    if (bar(Foo(), 42) != 43) return "Fail 5"

    return FromJava.test(bar)
}
