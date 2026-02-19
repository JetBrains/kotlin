// DISABLE_JAVA_FACADE
// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FILE: A.java
import java.util.*;

public class A<T> {
    public void foo(Err<String> x, List<String> y);
}

// FILE: B.java

public class B extends A {

}
