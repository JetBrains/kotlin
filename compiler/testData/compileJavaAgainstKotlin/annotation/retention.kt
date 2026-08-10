// FILE: retention.java
package test;

@Runtime @Source
class Test {}

// FILE: retention.kt
package test

@Retention(AnnotationRetention.RUNTIME)
annotation class Runtime

@Retention(AnnotationRetention.SOURCE)
annotation class Source
