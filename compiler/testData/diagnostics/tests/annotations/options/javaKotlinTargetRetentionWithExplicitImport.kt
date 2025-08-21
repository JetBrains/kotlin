// RUN_PIPELINE_TILL: BACKEND
import java.lang.annotation.Target
import java.lang.annotation.ElementType.PACKAGE

<!DEPRECATED_JAVA_ANNOTATION!>@Target(PACKAGE)<!>
annotation class my

/* GENERATED_FIR_TAGS: annotationDeclaration, javaProperty */
