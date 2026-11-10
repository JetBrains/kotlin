// TARGET_BACKEND: JVM
// WITH_REFLECT

// FILE: test/JavaCharSequence.java
package test;

public final class JavaCharSequence implements CharSequence {
    @Override
    public int length() {
        return 2;
    }

    @Override
    public char charAt(int index) {
        return "OK".charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return "OK".subSequence(start, end);
    }

    @Override
    public String toString() {
        return "OK";
    }
}

// FILE: test/JavaNumber.java
package test;

public final class JavaNumber extends Number {
    @Override
    public byte byteValue() {
        return 1;
    }

    @Override
    public short shortValue() {
        return 1;
    }

    @Override
    public int intValue() {
        return 1;
    }

    @Override
    public long longValue() {
        return 1L;
    }

    @Override
    public float floatValue() {
        return 1.0f;
    }

    @Override
    public double doubleValue() {
        return 1.0;
    }
}

// FILE: test/JavaCharSequenceWithKotlinSuperClass.java
package test;

public final class JavaCharSequenceWithKotlinSuperClass extends KotlinAbstractCharSequence {
    @Override
    public int getLength() {
        return 2;
    }

    @Override
    public char get(int index) {
        return "OK".charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return "OK".subSequence(start, end);
    }
}

// FILE: box.kt
package test

import kotlin.test.assertEquals
import kotlin.test.assertTrue

abstract class KotlinAbstractCharSequence : CharSequence

fun box(): String {
    assertEquals(
        "fun test.JavaCharSequence.get(kotlin.Int): kotlin.Char",
        JavaCharSequence::class.members.single { it.name == "get" }.toString(),
    )
    assertEquals(
        "val test.JavaCharSequence.length: kotlin.Int",
        JavaCharSequence::class.members.single { it.name == "length" }.toString(),
    )
    assertEquals(
        "fun test.JavaCharSequence.subSequence(kotlin.Int, kotlin.Int): kotlin.CharSequence",
        JavaCharSequence::class.members.single { it.name == "subSequence" }.toString(),
    )
    assertEquals('O', JavaCharSequence::get.call(JavaCharSequence(), 0))
    assertEquals(2, JavaCharSequence::class.members.single { it.name == "length" }.call(JavaCharSequence()))
    assertTrue(JavaCharSequence::class.members.none { it.name == "charAt" })

    val m = JavaCharSequenceWithKotlinSuperClass::class.members
    assertEquals('O', m.single { it.name == "get" }.call(JavaCharSequenceWithKotlinSuperClass(), 0))
    assertEquals(2, m.single { it.name == "length" }.call(JavaCharSequenceWithKotlinSuperClass()))
    assertTrue(m.none { it.name == "getLength" || it.name == "charAt" })

    assertEquals(
        "fun test.JavaNumber.toInt(): kotlin.Int",
        JavaNumber::class.members.single { it.name == "toInt" }.toString(),
    )
    assertEquals(1, JavaNumber::class.members.single { it.name == "toInt" }.call(JavaNumber()))
    assertTrue(JavaNumber::class.members.none { it.name == "intValue" })

    return "OK"
}
