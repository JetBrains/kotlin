// FILE: MyAnno.kt
@Target(AnnotationTarget.TYPE)
annotation class MyAnno(val s: String)

// FILE: JavaClass.java

class JavaClass {
    void materialize(@MyAnno("outer type") List<@MyAnno("middle") List<@MyAnno("nested") String>> @MyAnno("array") [] va<caret>lues) {}
}
