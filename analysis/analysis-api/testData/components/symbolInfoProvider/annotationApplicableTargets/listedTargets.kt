import kotlin.annotation.AnnotationTarget
import kotlin.annotation.Target

@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
annotation class A

@<caret>A object Foo