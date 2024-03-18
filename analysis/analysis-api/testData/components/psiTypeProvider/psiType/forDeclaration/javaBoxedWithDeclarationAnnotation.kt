// FILE: usage.kt
fun usa<caret>ge(j: JavaDeclaration) = j.foo()

@Target(AnnotationTarget.FUNCTION)
annotation class MyAnno(val s: String)

// FILE: JavaDeclaration.java
public interface JavaDeclaration {
    @MyAnno("java text") Integer foo();
}