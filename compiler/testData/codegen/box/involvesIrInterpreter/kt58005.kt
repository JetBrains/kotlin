// TARGET_BACKEND: JVM

// FILE: ComponentScans.java
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ComponentScans {
    ComponentScan[] value();
}

// FILE: ComponentScan.java
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(ComponentScans.class)
public @interface ComponentScan {
    String[] a() default {};
    String[] b() default {};
    String[] c() default {};
}

// FILE: main.kt
@ComponentScans(
    value = [
        ComponentScan(
            a = ["String" <!EVALUATED("StringA")!>+ "A"<!>],
            c = ["String" <!EVALUATED("StringC")!>+ "C"<!>],
            b = ["String" <!EVALUATED("StringB")!>+ "B"<!>],
        )
    ]
)
class JavaTest

annotation class KtComponentScans(
    val value: Array<KtComponentScan> = [],
)

annotation class KtComponentScan(
    val a: Array<String> = [],
    val b: Array<String> = [],
    val c: Array<String> = [],
)

@ComponentScans(
    value = [
        ComponentScan(
            a = ["String" <!EVALUATED("StringA")!>+ "A"<!>],
            c = ["String" <!EVALUATED("StringC")!>+ "C"<!>],
            b = ["String" <!EVALUATED("StringB")!>+ "B"<!>],
        )
    ]
)
class KtTest

fun box(): String {
    return "OK"
}
