// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +FixationEnhancementsIn22
// FULL_JDK
// STDLIB_JDK8
// JVM_TARGET: 1.8
// ISSUE: KT-71854
// DUMP_INFERENCE_LOGS: FIXATION

// FILE: Wrapper.java

import java.util.Comparator;
import java.util.Collections;

public class Wrapper {
    public static <T extends Comparable<? super T>> Comparator<T> reverseOrder() {
        return Collections.reverseOrder();
    }
}

// FILE: flatMapWithReverseOrder.kt
import java.util.TreeSet

class MergeFragment {
    val tailsAndBody = mutableSetOf<Int>()
}

fun specifyTypeTest(fragments: Set<MergeFragment>) {
    val flatMap = fragments.flatMapTo(TreeSet(Wrapper.reverseOrder())) { f -> f.tailsAndBody }

    for (f in flatMap) {
        testFun(<!ARGUMENT_TYPE_MISMATCH!>f<!>)
    }

}

fun testFun(i: Int) {}

/* GENERATED_FIR_TAGS: classDeclaration, flexibleType, forLoop, functionDeclaration, javaFunction, lambdaLiteral,
localProperty, propertyDeclaration */
