// Issue: KT-35856

// FILE: Bar.java
@Anno
public interface Bar {}

// FILE: Foo.kt
@Target(AnnotationTarget.FILE, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Anno

class Foo : Bar
