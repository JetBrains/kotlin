// FILE: JavaAnno.java
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.TYPE_USE)
public @interface JavaAnno {
    int number();
    String[] strings() default {};
    ElementType target();
    Class<?> klass();
}

// FILE: JavaClass.java
import java.lang.annotation.ElementType;

public class JavaClass {
    public static final int CONST = 5;

    public static @JavaAnno(number = CONST + 1, strings = {"a", "b"}, target = ElementType.FIELD, klass = String.class) String foo() {
        return "";
    }
}

// FILE: main.kt
fun test() {
    val x = <expr>JavaClass.foo()</expr>
}
