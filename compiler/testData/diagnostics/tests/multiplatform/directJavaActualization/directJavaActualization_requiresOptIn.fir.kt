// WITH_STDLIB

// MODULE: m1-common
// FILE: common.kt

<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect annotation class <!CLASSIFIER_REDECLARATION!>Foo<!><!>

// MODULE: m2-jvm()()(m1-common)
// FILE: Foo.java
@kotlin.RequiresOptIn
@kotlin.jvm.KotlinActual
public @interface Foo {}
