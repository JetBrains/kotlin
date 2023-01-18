// DO_NOT_CHECK_SYMBOL_RESTORE_K1
// WITH_STDLIB
// FILE: main.kt
fun some() {
    val jClass = JavaClass()
    jClass.<caret>field;
}

// FILE: JavaClass.java
public class JavaClass {
    public @Anno1 String field = 1;
}

// FILE: Anno1.java
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.TYPE_USE)
public @interface Anno1 {

}
