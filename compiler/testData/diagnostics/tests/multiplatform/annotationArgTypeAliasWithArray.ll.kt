// ISSUES: KT-61100, KT-59561
// WITH_STDLIB
// MODULE: m1-common
// FILE: common.kt

<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}, NO_ACTUAL_FOR_EXPECT{JVM}!>expect enum class Mode {
    Throughput, AverageTime
}<!>

expect annotation class BenchmarkMode(vararg val value: Mode)

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

actual typealias <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS("actual typealias Mode = Mode;     expect constructor(): Mode")!>Mode<!> = mypackage.Mode


actual typealias BenchmarkMode = mypackage.BenchmarkMode

// FILE: mypackage/Mode.java

package mypackage;

public enum Mode {
    Throughput, AverageTime
}

// FILE: mypackage/BenchmarkMode.java

package mypackage;

public @interface BenchmarkMode {

    Mode[] value();
}
