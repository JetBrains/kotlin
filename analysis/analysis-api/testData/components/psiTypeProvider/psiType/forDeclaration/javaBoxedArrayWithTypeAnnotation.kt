// FILE: usage.kt
fun usa<caret>ge(j: JavaDeclaration) = j.foo()

@Target(AnnotationTarget.TYPE)
annotation class MyAnno(val s: String)

// FILE: JavaDeclaration.java
public interface JavaDeclaration {
    @MyAnno("array value annotation") Integer @MyAnno("array annotation") [] foo();
}