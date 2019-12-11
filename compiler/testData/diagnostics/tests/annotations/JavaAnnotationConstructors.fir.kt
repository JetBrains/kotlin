import java.lang.annotation.Retention
import java.lang.annotation.Target
import java.lang.annotation.*

@java.lang.annotation.Retention(RetentionPolicy.CLASS)
annotation class my

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR)
annotation class my1