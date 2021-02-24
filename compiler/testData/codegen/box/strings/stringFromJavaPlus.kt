// TARGET_BACKEND: JVM
// WITH_RUNTIME

// FILE: J.java
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class J {
    public static String platformStringIsNull() {
        return null;
    }

    @NotNull
    public static String notNullStringIsNull() {
        return null;
    }

    @Nullable
    public static String nullableStringIsNull() {
        return null;
    }

    @NotNull
    public static String notNullStringIsNotNull() {
        return "foo";
    }

    public static String platformStringIsNotNull() {
        return "foo";
    }

    @Nullable
    public static String nullableStringIsNotNull() {
        return "foo";
    }
}

// FILE: stringFromJavaPlus.kt
import kotlin.test.assertEquals

fun box(): String {
    val n = 123

    // Null check behavior in string concatenation might change depending on language design decision for KT-36625.
    // Cases below that could be affected by KT-36625 are marked with a comment.

    assertEquals("null", "${J.platformStringIsNull()}")
    assertEquals("nullBAR", J.platformStringIsNull() + "BAR")  // KT-36625
    assertEquals("nullBAR", "${J.platformStringIsNull() + "BAR"}")  // KT-36625
    assertEquals("nullBAR", "${J.platformStringIsNull()}BAR")
    assertEquals("BARnull", "BAR" + J.platformStringIsNull())
    assertEquals("BARnull", "BAR${J.platformStringIsNull()}")
    assertEquals("123null", "$n${J.platformStringIsNull()}")
    assertEquals("nullBAR123", J.platformStringIsNull() + "BAR" + n)  // KT-36625
    assertEquals("nullBAR123", "${J.platformStringIsNull() + "BAR" + n}")  // KT-36625
    assertEquals("nullBAR123", "${J.platformStringIsNull()}BAR$n")
    assertEquals("BARnull123", "BAR" + J.platformStringIsNull() + n)
    assertEquals("BARnull123", "BAR${J.platformStringIsNull()}$n")
    assertEquals("BARnull123", "BAR" + (J.platformStringIsNull() + n))  // KT-36625
    assertEquals("123nullBAR", "$n${J.platformStringIsNull() + "BAR"}")  // KT-36625

    assertEquals("null", "${J.notNullStringIsNull()}")
    assertEquals("nullBAR", J.notNullStringIsNull() + "BAR")  // KT-36625
    assertEquals("nullBAR", "${J.notNullStringIsNull() + "BAR"}")  // KT-36625
    assertEquals("nullBAR", "${J.notNullStringIsNull()}BAR")
    assertEquals("BARnull", "BAR" + J.notNullStringIsNull())
    assertEquals("BARnull", "BAR${J.notNullStringIsNull()}")
    assertEquals("123null", "$n${J.notNullStringIsNull()}")
    assertEquals("nullBAR123", J.notNullStringIsNull() + "BAR" + n)  // KT-36625
    assertEquals("nullBAR123", "${J.notNullStringIsNull() + "BAR" + n}")  // KT-36625
    assertEquals("nullBAR123", "${J.notNullStringIsNull()}BAR$n")
    assertEquals("BARnull123", "BAR" + J.notNullStringIsNull() + n)
    assertEquals("BARnull123", "BAR${J.notNullStringIsNull()}$n")
    assertEquals("BARnull123", "BAR" + (J.notNullStringIsNull() + n))  // KT-36625
    assertEquals("123nullBAR", "$n${J.notNullStringIsNull() + "BAR"}")  // KT-36625

    assertEquals("null", "${J.nullableStringIsNull()}")
    assertEquals("nullBAR", J.nullableStringIsNull() + "BAR")
    assertEquals("nullBAR", "${J.nullableStringIsNull() + "BAR"}")
    assertEquals("nullBAR", "${J.nullableStringIsNull()}BAR")
    assertEquals("BARnull", "BAR" + J.nullableStringIsNull())
    assertEquals("BARnull", "BAR${J.nullableStringIsNull()}")
    assertEquals("123null", "$n${J.nullableStringIsNull()}")
    assertEquals("nullBAR123", J.nullableStringIsNull() + "BAR" + n)
    assertEquals("nullBAR123", "${J.nullableStringIsNull() + "BAR" + n}")
    assertEquals("nullBAR123", "${J.nullableStringIsNull()}BAR$n")
    assertEquals("BARnull123", "BAR" + J.nullableStringIsNull() + n)
    assertEquals("BARnull123", "BAR${J.nullableStringIsNull()}$n")
    assertEquals("BARnull123", "BAR" + (J.nullableStringIsNull() + n))
    assertEquals("123nullBAR", "$n${J.nullableStringIsNull() + "BAR"}")

    assertEquals("foo", "${J.platformStringIsNotNull()}")
    assertEquals("fooBAR", J.platformStringIsNotNull() + "BAR")
    assertEquals("fooBAR", "${J.platformStringIsNotNull() + "BAR"}")
    assertEquals("fooBAR", "${J.platformStringIsNotNull()}BAR")
    assertEquals("BARfoo", "BAR" + J.platformStringIsNotNull())
    assertEquals("BARfoo", "BAR${J.platformStringIsNotNull()}")
    assertEquals("123foo", "$n${J.platformStringIsNotNull()}")
    assertEquals("fooBAR123", J.platformStringIsNotNull() + "BAR" + n)
    assertEquals("fooBAR123", "${J.platformStringIsNotNull() + "BAR" + n}")
    assertEquals("fooBAR123", "${J.platformStringIsNotNull()}BAR$n")
    assertEquals("BARfoo123", "BAR" + J.platformStringIsNotNull() + n)
    assertEquals("BARfoo123", "BAR${J.platformStringIsNotNull()}$n")
    assertEquals("BARfoo123", "BAR" + (J.platformStringIsNotNull() + n))
    assertEquals("123fooBAR", "$n${J.platformStringIsNotNull() + "BAR"}")

    assertEquals("foo", "${J.notNullStringIsNotNull()}")
    assertEquals("fooBAR", J.notNullStringIsNotNull() + "BAR")
    assertEquals("fooBAR", "${J.notNullStringIsNotNull() + "BAR"}")
    assertEquals("fooBAR", "${J.notNullStringIsNotNull()}BAR")
    assertEquals("BARfoo", "BAR" + J.notNullStringIsNotNull())
    assertEquals("BARfoo", "BAR${J.notNullStringIsNotNull()}")
    assertEquals("123foo", "$n${J.notNullStringIsNotNull()}")
    assertEquals("fooBAR123", J.notNullStringIsNotNull() + "BAR" + n)
    assertEquals("fooBAR123", "${J.notNullStringIsNotNull() + "BAR" + n}")
    assertEquals("fooBAR123", "${J.notNullStringIsNotNull()}BAR$n")
    assertEquals("BARfoo123", "BAR" + J.notNullStringIsNotNull() + n)
    assertEquals("BARfoo123", "BAR${J.notNullStringIsNotNull()}$n")
    assertEquals("BARfoo123", "BAR" + (J.notNullStringIsNotNull() + n))
    assertEquals("123fooBAR", "$n${J.notNullStringIsNotNull() + "BAR"}")

    assertEquals("foo", "${J.nullableStringIsNotNull()}")
    assertEquals("fooBAR", J.nullableStringIsNotNull() + "BAR")
    assertEquals("fooBAR", "${J.nullableStringIsNotNull() + "BAR"}")
    assertEquals("fooBAR", "${J.nullableStringIsNotNull()}BAR")
    assertEquals("BARfoo", "BAR" + J.nullableStringIsNotNull())
    assertEquals("BARfoo", "BAR${J.nullableStringIsNotNull()}")
    assertEquals("123foo", "$n${J.nullableStringIsNotNull()}")
    assertEquals("fooBAR123", J.nullableStringIsNotNull() + "BAR" + n)
    assertEquals("fooBAR123", "${J.nullableStringIsNotNull() + "BAR" + n}")
    assertEquals("fooBAR123", "${J.nullableStringIsNotNull()}BAR$n")
    assertEquals("BARfoo123", "BAR" + J.nullableStringIsNotNull() + n)
    assertEquals("BARfoo123", "BAR${J.nullableStringIsNotNull()}$n")
    assertEquals("BARfoo123", "BAR" + (J.nullableStringIsNotNull() + n))
    assertEquals("123fooBAR", "$n${J.nullableStringIsNotNull() + "BAR"}")

    return "OK"
}
