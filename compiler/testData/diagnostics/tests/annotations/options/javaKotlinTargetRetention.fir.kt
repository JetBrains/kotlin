import java.lang.annotation.*

@java.lang.annotation.Target(ElementType.PACKAGE)
@Target(AnnotationTarget.CLASS)
annotation class my

@java.lang.annotation.Retention(RetentionPolicy.SOURCE)
@Retention(AnnotationRetention.BINARY)
annotation class your
