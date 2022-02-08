// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// WITH_STDLIB
// FULL_JDK
import java.util.stream.*

fun test() =
    IntStream.of(1, 1, 4, 5, 1, 4)
        .map { a: Int -> a + 1 }
        .map { a: Int -> a - 1 }
        .filter { a: Int -> a > 3 }
        .flatMap { a: Int -> IntStream.of(a, a, a) }
        .boxed()
        .collect(Collectors.toList())

// JVM_IR_TEMPLATES
// 4 INVOKEDYNAMIC
// 0 class StreamApiKt\$test\$

// JVM_TEMPLATES
// 0 INVOKEDYNAMIC
// 4 class StreamApiKt\$test\$
