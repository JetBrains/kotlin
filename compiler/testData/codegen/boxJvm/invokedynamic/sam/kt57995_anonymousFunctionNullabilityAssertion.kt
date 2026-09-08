// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_STDLIB
// FULL_JDK
// SAM_CONVERSIONS: INDY
// LAMBDAS: CLASS

// CHECK_BYTECODE_TEXT
// 3 java/lang/invoke/LambdaMetafactory
// 0 \$sam\$

// Anonymous functions keep LOCAL_FUNCTION, so codegen cannot recognize indy use by origin. Check parameter
// assertions for both widened and direct SAM conversions.

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
    val projected = exposeForJava<String>(fun(_: String) { events.append("projected") })
    if (J.bodyRanWithNull(projected)) return "Fail: null reached the non-null anonymous-function body: $events"

    val plain = Consumer(fun(_: String) { events.append("plain") })
    if (J.bodyRanWithNull(plain)) return "Fail: null reached the plain anonymous-function body: $events"

    if (events.isNotEmpty()) return "Fail: a body ran before the null check: $events"

    val nullable = exposeForJava<String?>(fun(value: String?) { events.append(if (value == null) "nullable" else "notNull") })
    if (!J.bodyRanWithNull(nullable)) return "Fail: null rejected by an anonymous function with a nullable parameter"
    if (events.toString() != "nullable") return "Fail: $events"

    return "OK"
}
