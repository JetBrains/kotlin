// FILE: usage.kt
fun usa<caret>ge(j: JavaDeclaration) = j.foo()

@Target(AnnotationTarget.TYPE)
annotation class MyAnno(val s: String)

// FILE: JavaDeclaration.java
import java.util.List;

public interface JavaDeclaration {
    @MyAnno("outer") List<@MyAnno("middle") List<@MyAnno("nested") String>> foo();
}
