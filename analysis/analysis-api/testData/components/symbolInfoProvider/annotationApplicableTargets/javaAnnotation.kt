// FILE: A.java

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
public @interface A {}

// FILE: Foo.kt
@<caret>A object Foo