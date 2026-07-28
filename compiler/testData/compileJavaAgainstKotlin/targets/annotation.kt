// FILE: annotation.java
package test;

@meta @interface MyAnn {

}

@meta class My {

}

// FILE: annotation.kt
package test

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class meta
