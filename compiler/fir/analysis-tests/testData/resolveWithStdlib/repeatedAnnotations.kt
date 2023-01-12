// FILE: Some.java
import java.util.List;

public class Some {
    public static List<@SomeAnn(1) @SomeAnn(2) String> foo() {
        return null;
    }
}

// FILE: SomeAnn.java

@Target({ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})
public @interface SomeAnn {
    int value();
}


// FILE: test.kt
fun <T> foo(bar: () -> T) {

}

@Target(AnnotationTarget.TYPE)
annotation class Ann

fun baz(): @Ann <!REPEATED_ANNOTATION!>@Ann<!> String = "12"
fun qux() = Some.foo()[0]

fun test() {
    foo({ Some.foo()[0] })
    foo({ baz() })
    foo({ qux() })
    foo(fun(): @Ann <!REPEATED_ANNOTATION!>@Ann<!> String {
        return ""
    })
}

