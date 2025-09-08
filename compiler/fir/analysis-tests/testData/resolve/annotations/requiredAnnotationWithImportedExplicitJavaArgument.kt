// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-75844

import java.lang.annotation.ElementType

<!ANNOTATION_TARGETS_ONLY_IN_JAVA, DEPRECATED_JAVA_ANNOTATION!>@java.lang.annotation.Target(ElementType.ANNOTATION_TYPE)<!>
annotation class MyAnnotation

/* GENERATED_FIR_TAGS: annotationDeclaration, javaProperty */
