// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 0 java/lang/invoke/LambdaMetafactory

// FILE: BiFunction.java

public interface BiFunction<T1, T2, R> {
  R apply(T1 t1, T2 t2);
}

// FILE: Operators.java

public class Operators {
    public static <T1, T2, R> R combine(T1 a, T2 b, BiFunction<T1, T2, R> combiner) {
        return combiner.apply(a, b);
    }
}

// FILE: main.kt

fun box() = Operators.combine("O", "K", String::plus)
