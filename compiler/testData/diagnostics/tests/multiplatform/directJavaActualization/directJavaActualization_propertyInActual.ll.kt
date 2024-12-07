// RUN_PIPELINE_TILL: FIR2IR
// WITH_KOTLIN_JVM_ANNOTATIONS
// LANGUAGE:+DirectJavaActualization
// ISSUES: KT-71809
// MODULE: m1-common
// FILE: common.kt
open class Base {
    val valOverriddenGetter: Int = 0
    val valOverriddenGetter_missingAnootation: Int = 0
    val valFakeOverrideGetter: Int = 0

    var varOverriddenOnlySetter: Int = 0
    var varOverriddenOnlySetter_missingAnnotation: Int = 0
    var varOverriddenOnlyGetter: Int = 0
    var varOverriddenOnlyGetter_missingAnnotation: Int = 0

    var varOverriddenGetterAndSetter: Int = 0
    var varOverriddenGetterAndSetter_missingOnlyGetterAnnotation: Int = 0
    var varOverriddenGetterAndSetter_missingOnlySetterAnnotation: Int = 0
    var varOverriddenGetterAndSetter_missingBothAnnotations: Int = 0
    var varFakeOverrideGetterAndSetter: Int = 0
}

expect class Foo {
    val valOverriddenGetter: Int
    val valOverriddenGetter_missingAnootation: Int
    val valFakeOverrideGetter: Int

    var varOverriddenOnlySetter: Int
    var varOverriddenOnlySetter_missingAnnotation: Int
    var varOverriddenOnlyGetter: Int
    var varOverriddenOnlyGetter_missingAnnotation: Int
    var varOverriddenGetterAndSetter: Int
    var varOverriddenGetterAndSetter_missingOnlyGetterAnnotation: Int
    var varOverriddenGetterAndSetter_missingOnlySetterAnnotation: Int
    var varOverriddenGetterAndSetter_missingBothAnnotations: Int
    var varFakeOverrideGetterAndSetter: Int
}

// MODULE: m2-jvm()()(m1-common)
// FILE: Foo.java
@kotlin.annotations.jvm.KotlinActual public class Foo extends Base {
    @kotlin.annotations.jvm.KotlinActual @Override public int getValOverriddenGetter() { return 0; }
    @Override public int getValOverriddenGetter_missingAnootation() { return 0; }

    @kotlin.annotations.jvm.KotlinActual @Override public void setVarOverriddenOnlySetter(int value) {}
    @Override public void setVarOverriddenOnlySetter_missingAnnotation(int value) {}
    @kotlin.annotations.jvm.KotlinActual @Override public int getVarOverriddenOnlyGetter() { return 0; }
    @Override public int getVarOverriddenOnlyGetter_missingAnnotation() { return 0; }

    @kotlin.annotations.jvm.KotlinActual @Override public int getVarOverriddenGetterAndSetter() { return 0; }
    @kotlin.annotations.jvm.KotlinActual @Override public void setVarOverriddenGetterAndSetter(int value) {}

    @Override public int getVarOverriddenGetterAndSetter_missingOnlyGetterAnnotation() { return 0; }
    @kotlin.annotations.jvm.KotlinActual @Override public void setVarOverriddenGetterAndSetter_missingOnlyGetterAnnotation(int value) {}

    @kotlin.annotations.jvm.KotlinActual @Override public int getVarOverriddenGetterAndSetter_missingOnlySetterAnnotation() { return 0; }
    @Override public void setVarOverriddenGetterAndSetter_missingOnlySetterAnnotation(int value)

    @Override public int getVarOverriddenGetterAndSetter_missingBothAnnotations() { return 0; }
    @Override public void setVarOverriddenGetterAndSetter_missingBothAnnotations(int value) {}
}
