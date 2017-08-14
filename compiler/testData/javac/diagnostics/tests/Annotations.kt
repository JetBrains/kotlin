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

    @def
    public String method3() { return null; }

    @def(1)
    public String method4() { return null; }

    @def(firstDefault = "f", 5)
    public String method5() { return null; }

    @def(secondDefault = "s", 14)
    public String method6() { return null; }

    @def(firstDefault = "f", secondDefault = "s", 17)
    public String method7() { return null; }

    @def("1", "2", 3)
    public String method8() { return null; }

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

// FILE: a/def.java
package a;

public @interface def {
    String firstDefault() default "firstDefault";
    String secondDefault() default "secondDefault";
    int notDefault();
}