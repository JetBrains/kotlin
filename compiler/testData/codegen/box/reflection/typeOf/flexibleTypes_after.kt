// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: box.kt

package test

import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlin.test.assertEquals

fun box(): String {
    assertEquals("kotlin.String!", returnTypeOf { J.nullabilityFlexible() }.toString())
    assertEquals("kotlin.collections.(Mutable)List<kotlin.String!>", returnTypeOf { J.mutabilityFlexible() }.toString())
    assertEquals("kotlin.collections.(Mutable)Map.(Mutable)Entry<kotlin.String!, kotlin.Int!>!", returnTypeOf { J.bothFlexible() }.toString())
    assertEquals("kotlin.Array<out kotlin.CharSequence!>!", returnTypeOf { J.arrayElementVarianceFlexible() }.toString())
    assertEquals("((kotlin.Number!) -> kotlin.Any!)!", returnTypeOf { J.function() }.toString())
    assertEquals("kotlin.Function1<kotlin.Any!, *>!", returnTypeOf { J.functionWithStar() }.toString())

    return "OK"
}

inline fun <reified T : Any> returnTypeOf(block: () -> T) =
    typeOf<T>()

// FILE: J.java

import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.Map;
import kotlin.jvm.functions.Function1;

public class J {
    public static String nullabilityFlexible() {
        return null;
    }

    @NotNull
    public static List<String> mutabilityFlexible() {
        return null;
    }

    public static Map.Entry<String, Integer> bothFlexible() {
        return null;
    }

    public static CharSequence[] arrayElementVarianceFlexible() {
        return null;
    }

    public static Function1<Number, Object> function() {
        return null;
    }

    public static Function1<Object, ?> functionWithStar() {
        return null;
    }
}
