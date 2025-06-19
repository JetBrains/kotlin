// FILE: MyAnno.kt
@Target(AnnotationTarget.TYPE)
annotation class MyAnno(val s: String)

// FILE: JavaClass.java
import java.util.List;

class JavaClass {
    void materialize(@MyAnno("outer") List<@MyAnno("middle") List<@MyAnno("nested") String>> @MyAnno("vararg") ...va<caret>lues) {}
}
