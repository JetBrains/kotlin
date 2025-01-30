// IGNORE_BACKEND_K1: JVM_IR
// ISSUE: KT-73256, KT-74382 (not supported in K1)
// LANGUAGE: +AnnotationAllUseSiteTarget +PropertyParamAnnotationDefaultTargetMode

// FILE: JavaFieldComponent.java

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.RECORD_COMPONENT, ElementType.FIELD })
public @interface JavaFieldComponent {
}

// FILE: JavaParamComponent.java

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.RECORD_COMPONENT, ElementType.PARAMETER })
public @interface JavaParamComponent {
}

// FILE: JavaParamFieldComponent.java

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.RECORD_COMPONENT, ElementType.PARAMETER, ElementType.FIELD })
public @interface JavaParamFieldComponent {
}

// FILE: JavaDefault.java

import java.lang.annotation.*;

// requires explicit @field target to be applied
@Retention(RetentionPolicy.RUNTIME)
public @interface JavaDefault {
}

// FILE: main.kt

@java.lang.annotation.Target(java.lang.annotation.ElementType.RECORD_COMPONENT)
@Target(AnnotationTarget.FIELD)
annotation class FieldComponent

@Target(AnnotationTarget.FIELD)
annotation class Field

// actually not applicable to records, since FIELD is not a valid target
@java.lang.annotation.Target(java.lang.annotation.ElementType.RECORD_COMPONENT)
@Target(AnnotationTarget.PROPERTY)
annotation class PropertyComponent

// no @Target means "every target"
annotation class Default

@java.lang.annotation.Target(java.lang.annotation.ElementType.RECORD_COMPONENT)
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
annotation class PropertyFieldComponent

@java.lang.annotation.Target(value = [])
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
annotation class NoneInJava

@JvmRecord
data class Some(
    @FieldComponent val x: Int,
    @Field val y: Int,
    @PropertyComponent val z: Int,
    @Default val u: Int,
    @field:Default val v: Int,
    @PropertyFieldComponent val w: Int,
    @NoneInJava val n: Int,
    @JavaFieldComponent val a: Int,
    @JavaParamComponent val b: Int,
    @JavaParamFieldComponent val c: Int,
    @JavaDefault val d: Int,
    @field:JavaDefault val e: Int,
)

@JvmRecord
data class Else(
    @all:FieldComponent val x: Int,
    @all:Field val y: Int,
    @all:PropertyComponent val z: Int,
    @all:Default val u: Int,
    @all:PropertyFieldComponent val w: Int,
    @all:NoneInJava val n: Int,
    @all:JavaFieldComponent val a: Int,
    @all:JavaParamFieldComponent val c: Int,
    @all:JavaDefault val d: Int,
)

fun box(): String {
    val someComponents = Some::class.java.recordComponents

    if (someComponents[0].annotations.isEmpty()) {
        return "FAIL: no record component annotation for '@FieldComponent val x' found"
    }
    if (someComponents[1].annotations.isNotEmpty()) {
        return "FAIL: record component annotation for '@Field val y' found, but it should not be so"
    }
    if (someComponents[2].annotations.isNotEmpty()) {
        return "FAIL: record component annotation for '@PropertyComponent val z' found, but it should not be so"
    }
    if (someComponents[3].annotations.isNotEmpty()) {
        return "FAIL: record component annotation for '@Default val u' found, but it should not be so"
    }
    if (someComponents[4].annotations.isEmpty()) {
        return "FAIL: no record component annotation for '@field:Default val v' found"
    }
    if (someComponents[5].annotations.isNotEmpty()) {
        return "FAIL: record component annotation for '@PropertyFieldComponent val w' found, but it should not be so"
    }
    if (someComponents[6].annotations.isNotEmpty()) {
        return "FAIL: record component annotation for '@NoneInJava val n' found, but it should not be so"
    }
    if (someComponents[7].annotations.isEmpty()) {
        return "FAIL: no record component annotation for '@JavaFieldComponent val a' found"
    }
    if (someComponents[8].annotations.isNotEmpty()) {
        return "FAIL: record component annotation for '@JavaParamComponent val b' found, but it should not be so"
    }
    if (someComponents[9].annotations.isEmpty()) {
        return "FAIL: no record component annotation for '@JavaParamFieldComponent val c' found"
    }
    if (someComponents[10].annotations.isNotEmpty()) {
        return "FAIL: record component annotation for '@JavaDefault val d' found, but it should not be so"
    }
    if (someComponents[11].annotations.isEmpty()) {
        return "FAIL: no record component annotation for '@field:JavaDefault val e' found"
    }

    val elseComponents = Else::class.java.recordComponents

    if (elseComponents[0].annotations.isEmpty()) {
        return "FAIL: no record component annotation for '@all:FieldComponent val x' found"
    }
    if (elseComponents[1].annotations.isNotEmpty()) {
        return "FAIL: record component annotation for '@all:Field val y' found, but it should not be so"
    }
    if (elseComponents[2].annotations.isNotEmpty()) {
        return "FAIL: record component annotation for '@all:PropertyComponent val z' found, but it should not be so"
    }
    if (elseComponents[3].annotations.isEmpty()) {
        return "FAIL: no record component annotation for '@all:Default val u' found"
    }
    if (elseComponents[4].annotations.isEmpty()) {
        return "FAIL: no record component annotation for '@all:PropertyFieldComponent val w' found"
    }
    if (elseComponents[5].annotations.isNotEmpty()) {
        return "FAIL: record component annotation for '@all:NoneInJava val n' found, but it should not be so"
    }
    if (elseComponents[6].annotations.isEmpty()) {
        return "FAIL: no record component annotation for '@all:JavaFieldComponent val a' found"
    }
    if (elseComponents[7].annotations.isEmpty()) {
        return "FAIL: no record component annotation for '@all:JavaParamFieldComponent val c' found"
    }
    if (elseComponents[8].annotations.isEmpty()) {
        return "FAIL: no record component annotation for '@all:JavaDefault val d' found"
    }

    return "OK"
}