// FILE: MyAnno.kt
@Target(AnnotationTarget.TYPE)
annotation class MyAnno(val s: String)

// FILE: JavaClass.java
class JavaClass {
    void materialize(@MyAnno("type") String @MyAnno("array") [] val<caret>ues) {}
}
