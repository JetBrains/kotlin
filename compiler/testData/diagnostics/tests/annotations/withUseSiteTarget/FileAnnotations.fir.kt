// FILE: annotations.kt
@Target(AnnotationTarget.CLASS)
public annotation class ClassAnn

@Target(AnnotationTarget.FILE)
public annotation class FileAnn

// FILE: 1.kt
@file:ClassAnn

// FILE: 2.kt
@file:FileAnn