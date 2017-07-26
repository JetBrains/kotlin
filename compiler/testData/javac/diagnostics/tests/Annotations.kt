// FILE: a/ann.java
package a;

public @interface ann {
    Class value();
}

// FILE: a/x.java
package a;

@ann(String.class)
public class x {
    @ann2(value = { @ann1(a = "a" + "b", i = 1 * 2), @ann1(a = "b", i = 2), @ann1(a = "c", i = 3) }, i = 42)
    public String method() { return null; }

    @ann3((1 + 1))
    public String method2() { return null; }
}

// FILE: a/ann1.java
package a;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@interface ann1 {
    public String a();
    public int i();
}
// FILE: a/ann2.java
package a;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@interface ann2 {
    public ann1[] value() default {};
    public int i();
}

// FILE: a/ann3.java
package a;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@interface ann3 {
    public int value();
}