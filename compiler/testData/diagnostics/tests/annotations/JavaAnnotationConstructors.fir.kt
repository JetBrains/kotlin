// RUN_PIPELINE_TILL: BACKEND
import java.lang.annotation.Retention
import java.lang.annotation.Target
import java.lang.annotation.*

<!DEPRECATED_JAVA_ANNOTATION!>@java.lang.annotation.Retention(RetentionPolicy.CLASS)<!>
annotation class my

<!DEPRECATED_JAVA_ANNOTATION!>@Retention(RetentionPolicy.RUNTIME)<!>
<!ANNOTATION_TARGETS_ONLY_IN_JAVA, DEPRECATED_JAVA_ANNOTATION!>@Target(ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR)<!>
annotation class my1