// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_STDLIB
// FULL_JDK
// SAM_CONVERSIONS: INDY

// CHECK_BYTECODE_TEXT
// 1 java/lang/invoke/LambdaMetafactory
// 0 \$sam\$

// FILE: SerFunction.java

import java.io.Serializable;
import java.util.function.Function;

public interface SerFunction<T, R> extends Function<T, R>, Serializable {
}

// FILE: box.kt

import java.io.*

fun roundTrip(x: Any?): Any? {
    val bos = ByteArrayOutputStream()
    ObjectOutputStream(bos).use { it.writeObject(x) }
    return ObjectInputStream(ByteArrayInputStream(bos.toByteArray())).readObject()
}

fun <T> useAfterRoundTrip(f: SerFunction<in T, String>, value: T): String {
    @Suppress("UNCHECKED_CAST")
    val g = roundTrip(f) as SerFunction<in T, String>
    return g.apply(value)
}

fun box(): String = useAfterRoundTrip<String>({ it + "K" }, "O")
