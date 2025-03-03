// RUN_PIPELINE_TILL: BACKEND
import java.lang.annotation.Target
import java.lang.annotation.ElementType.PACKAGE

<!ANNOTATION_TARGETS_ONLY_IN_JAVA, DEPRECATED_JAVA_ANNOTATION!>@Target(PACKAGE)<!>
annotation class my
