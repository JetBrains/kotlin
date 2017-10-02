// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

expect annotation class Foo1
expect annotation class Foo2
expect annotation class Foo3
expect annotation class Foo4

// MODULE: m2-jvm(m1-common)

// FILE: Bar1.java

public @interface Bar1 {
    String value() default "";
}

// FILE: Bar2.java

public @interface Bar2 {
    String value() default "";
    String path();
}

// FILE: jvm.kt

actual typealias Foo1 = Bar1

<!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>actual typealias Foo4 = Bar2<!>

actual annotation class Foo2(val p: String = "default")

actual annotation class Foo3(val a: String = "a", val b: String = "b")