// FILE: MyAnno.kt
@Target(AnnotationTarget.TYPE)
annotation class MyAnno(val s: String)

// FILE: JavaClass.java
class JavaClass {
    void materialize(int @MyAnno("array") [] va<caret>lues) {}
}
