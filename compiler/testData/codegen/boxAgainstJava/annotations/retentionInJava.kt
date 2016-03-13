// FILE: Foo.java

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.CLASS)
@interface Foo {
}

// FILE: 1.kt

@Foo class Bar

fun box(): String {
    Bar()
    return "OK"
}
