// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_STDLIB
// FULL_JDK
// SAM_CONVERSIONS: INDY
// LAMBDAS: CLASS

// CHECK_BYTECODE_TEXT
// 2 java/lang/invoke/LambdaMetafactory
// 0 \$sam\$

// A SAM type with an 'in' projection is converted to a single invokedynamic, which requires widening the
// implementation lambda's parameter to the instantiated method parameter type. That must not drop the
// non-null assertion on the lambda parameter (KT-44278), including when the parameter is unused.

// FILE: J.java
import java.util.function.Consumer;

public final class J {
    public static boolean bodyRanWithNull(Consumer<? super String> consumer) {
        try {
            consumer.accept(null);
        } catch (NullPointerException expected) {
            return false;
        }
        return true;
    }
}

// FILE: box.kt
import java.util.function.Consumer

val events = StringBuilder()

fun <T> exposeForJava(consumer: Consumer<in T>): Consumer<in T> = consumer

fun box(): String {
    // 'it' is non-null and unused: the null check still has to run, before the body.
    val nonNull = exposeForJava<String>({ events.append("nonNull") })
    if (J.bodyRanWithNull(nonNull)) return "Fail: null reached the non-null lambda body: $events"
    if (events.isNotEmpty()) return "Fail: body ran before the null check: $events"

    // 'it' is nullable: null must reach the body as before.
    val nullable = exposeForJava<String?>({ events.append(if (it == null) "nullable" else "notNull") })
    if (!J.bodyRanWithNull(nullable)) return "Fail: null rejected by a lambda with a nullable parameter"
    if (events.toString() != "nullable") return "Fail: $events"

    return "OK"
}
