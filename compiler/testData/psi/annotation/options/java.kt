import java.lang.annotation.*

annotation 
@java.lang.annotation.Retention(RetentionPolicy.CLASS) 
class my

annotation 
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR)
class my1
