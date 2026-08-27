// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-88667
// FILE: TestAnn.java
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface TestAnn {
    String message();
}
// FILE: test.kt
import java.lang.Deprecated as deprecated

@TestAnn(<!POSITIONED_VALUE_ARGUMENT_FOR_JAVA_ANNOTATION!>"message"<!>)
class A {
    @get:TestAnn(<!POSITIONED_VALUE_ARGUMENT_FOR_JAVA_ANNOTATION!>"message"<!>) <!DEPRECATED_JAVA_ANNOTATION!>@get:deprecated<!>
    val x: Int = 10
    fun test() = TestAnn(<!POSITIONED_VALUE_ARGUMENT_FOR_JAVA_ANNOTATION!>"message"<!>)
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetPropertyGetter, classDeclaration, functionDeclaration, integerLiteral,
interfaceDeclaration, javaProperty, propertyDeclaration, stringLiteral */
