// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -AnnotationDefaultTargetMigrationWarning -PropertyParamAnnotationDefaultTargetMode -ForbidFieldAnnotationsOnAnnotationParameters
// ISSUE: KT-73255 KT-73831
// FILE: JavaAnn.java
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
public @interface JavaAnn {
    String value() default "OK";
}

// FILE: test.kt
import JavaAnn

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class ParamOnly

@Target(AnnotationTarget.PROPERTY)
annotation class PropertyOnly

@Target(AnnotationTarget.FIELD)
annotation class FieldOnly

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY)
annotation class ParamProperty

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD)
annotation class ParamField

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
annotation class PropertyField

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
annotation class ParamPropertyField

@Target(AnnotationTarget.CLASS)
annotation class Inapplicable

class My(
    @ParamOnly
    @PropertyOnly
    @FieldOnly
    @ParamProperty
    @ParamField
    @PropertyField
    @ParamPropertyField
    <!WRONG_ANNOTATION_TARGET!>@Inapplicable<!>
    @JavaAnn
    val x: Int,
    @ParamOnly
    @PropertyOnly
    @FieldOnly
    @ParamProperty
    @ParamField
    @PropertyField
    @ParamPropertyField
    <!WRONG_ANNOTATION_TARGET!>@Inapplicable<!>
    @JavaAnn
    vararg val a: String
) {
    <!WRONG_ANNOTATION_TARGET!>@ParamOnly<!>
    @PropertyOnly
    @FieldOnly
    @ParamProperty
    @ParamField
    @PropertyField
    @ParamPropertyField
    <!WRONG_ANNOTATION_TARGET!>@Inapplicable<!>
    @JavaAnn
    val y: Int = 0

    <!WRONG_ANNOTATION_TARGET!>@ParamOnly<!>
    @PropertyOnly
    <!WRONG_ANNOTATION_TARGET!>@FieldOnly<!>
    @ParamProperty
    <!WRONG_ANNOTATION_TARGET!>@ParamField<!>
    @PropertyField
    @ParamPropertyField
    <!WRONG_ANNOTATION_TARGET!>@Inapplicable<!>
    <!WRONG_ANNOTATION_TARGET!>@JavaAnn<!>
    val z: Int get() = 0
}

annotation class Your(
    @ParamOnly
    @PropertyOnly
    <!WRONG_ANNOTATION_TARGET_WARNING!>@FieldOnly<!>
    @ParamProperty
    @ParamField
    @PropertyField
    @ParamPropertyField
    <!WRONG_ANNOTATION_TARGET!>@Inapplicable<!>
    @JavaAnn
    val s: String
)
