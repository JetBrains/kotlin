// FILE: A.java
public class A {}

// FILE: X.java
import org.jetbrains.annotations.NotNull;

public class X<T> {
    @NotNull T fooN() {return null;}
    void barN(@NotNull T a) {}
}

// FILE: Y.java
public class Y extends X<A> {

}

// FILE: test.kt

fun main() {
    Y().fooN() : Any
    Y().barN(<!ERROR_COMPILE_TIME_VALUE!>null<!>);
}

