// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// WITH_STDLIB

// MODULE: m1-common
// FILE: common.kt
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.<!UNRESOLVED_REFERENCE!>ExperimentalAtomicApi<!>

<!OPT_IN_WITHOUT_ARGUMENTS!>@OptIn(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!><!UNRESOLVED_REFERENCE!>ExperimentalAtomicApi<!>::class<!>)<!>
expect class Foo {
    fun test(a: AtomicInt): AtomicInt
}

// MODULE: m2-jvm()()(m1-common)
// FILE: Bar.java
import java.util.concurrent.atomic.AtomicInteger;

public class Bar {
    public AtomicInteger test(AtomicInteger i){
        return i;
    }
}

//FILE: test.kt
actual typealias <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>Foo<!> = Bar
