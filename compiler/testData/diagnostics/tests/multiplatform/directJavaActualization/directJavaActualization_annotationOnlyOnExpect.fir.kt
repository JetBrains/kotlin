// WITH_KOTLIN_JVM_ANNOTATIONS
// MODULE: m1-common
// FILE: common.kt

annotation class Annot

<!NO_ACTUAL_FOR_EXPECT{JVM}!>@Annot
expect class <!CLASSIFIER_REDECLARATION!>Foo<!><!>

// MODULE: m2-jvm()()(m1-common)
// FILE: Foo.java
@kotlin.annotations.jvm.KotlinActual
public class Foo {}
