import java.lang.annotation.*

<!DEPRECATED_JAVA_ANNOTATION!>@java.lang.annotation.Target(ElementType.PACKAGE)<!>
@Target(AnnotationTarget.CLASS)
annotation class my

<!DEPRECATED_JAVA_ANNOTATION!>@java.lang.annotation.Retention(RetentionPolicy.SOURCE)<!>
@Retention(AnnotationRetention.BINARY)
annotation class your
