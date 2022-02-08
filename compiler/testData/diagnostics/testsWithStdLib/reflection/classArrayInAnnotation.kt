// FIR_IDENTICAL
// SKIP_TXT
// FILE: MyAnn.java
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MyAnn {
    /**
     * @return the classes to be run
     */
    public Class<?>[] value();
}
// FILE: main.kt

fun foo(y: MyAnn?): List<Class<*>>? {
    return y?.value?.map { it.java }
}
