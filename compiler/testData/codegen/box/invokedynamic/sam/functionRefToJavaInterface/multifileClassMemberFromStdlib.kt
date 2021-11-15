// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 1 java/lang/invoke/LambdaMetafactory

// IGNORE_BACKEND_FIR: JVM_IR
//  ^ OVERLOAD_RESOLUTION_AMBIGUITY: Overload resolution ambiguity between candidates: [kotlin/collections/plus, kotlin/collections/plus]

// FILE: multifileClassMemberFromStdlib.kt

fun test(a: List<String>, b: List<String>, bf: BF) =
    bf.apply(a, b)

fun box(): String {
    val ss = test(listOf("O"), listOf("K"), List<String>::plus)
    return ss[0] + ss[1]
}

// FILE: BF.java
import java.util.*;

public interface BF {
    List<String> apply(List<String> a, List<String> b);
}
