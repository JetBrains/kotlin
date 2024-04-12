// LANGUAGE: -JavaTypeParameterDefaultRepresentationWithDNN

// FILE: JavaGeneric.java
public class JavaGeneric<T> {

    public JavaBox<?> foo(JavaBox<?> a) { return null; }

    public JavaBox<? super T> foo2(JavaBox<? super T> a) { return null; }

    public JavaBox<? super T> foo3(T a) { return null; }

    public JavaBox<? extends T> foo4(JavaBox<? extends T> a) { return null; }

    public JavaBox<? extends JavaBox<T>> foo5(JavaBox<? extends JavaBox<T>> a) { return null; }

    public JavaBox<? super JavaBox<T>> foo6(JavaBox<? super JavaBox<T>> a) { return null; }

    public JavaBox<T> foo7(JavaBox<T> a) { return null; }
}

// FILE: JavaBox.java
public class JavaBox<T> {
    public JavaBox(T b) { a = b; }
    public T a;
}

// FILE: Test.kt

fun genericClassTest(a1: JavaGeneric<String>,
                     a2: JavaGeneric<String?>) {
    val k1: JavaBox<*> = a1.foo(JavaBox(null))
    val k2: Any = a1.foo(JavaBox(null)).a

    val k3: JavaBox<in String> = a1.foo2(JavaBox(null))
    val k4: Any = <!TYPE_MISMATCH!>a1.foo2(JavaBox(null)).a<!>

    val k5: JavaBox<in String> = a1.foo3(null)
    val k6: Any = <!TYPE_MISMATCH!>a1.foo3(null).a<!>

    val k7: JavaBox<out String> = a1.foo4(JavaBox(null))
    val k8: Any = a1.foo4(JavaBox(null)).a

    val k9: JavaBox<out JavaBox<String>> = a1.foo5(JavaBox(null))
    val k10: Any = a1.foo5(JavaBox(null)).a

    val k11: JavaBox<in JavaBox<String>> = a1.foo6(JavaBox(null))
    val k12: Any = <!TYPE_MISMATCH!>a1.foo6(JavaBox(null)).a<!>

    val k13: JavaBox<String> = a1.foo7(JavaBox(null))
    val k14: Any = a1.foo7(JavaBox(null)).a

    val k15: JavaBox<*> = a2.foo(JavaBox(null))
    val k16: Any = a2.foo(JavaBox(null)).a

    val k17: JavaBox<in String> = a2.foo2(JavaBox(null))
    val k18: Any = <!TYPE_MISMATCH!>a2.foo2(JavaBox(null)).a<!>

    val k19: JavaBox<in String> = a2.foo3(null)
    val k20: Any = <!TYPE_MISMATCH!>a2.foo3(null).a<!>

    val k21: JavaBox<out String> = a2.foo4(JavaBox(null))
    val k22: Any = <!TYPE_MISMATCH!>a2.foo4(JavaBox(null)).a<!>

    val k23: JavaBox<out JavaBox<String?>> = a2.foo5(JavaBox(null))
    val k24: Any = a2.foo5(JavaBox(null)).a

    val k25: JavaBox<in JavaBox<String?>> = a2.foo6(JavaBox(null))
    val k26: Any = <!TYPE_MISMATCH!>a2.foo6(JavaBox(null)).a<!>

    val k27: JavaBox<String?> = a2.foo7(JavaBox(null))
    val k28: Any = <!TYPE_MISMATCH!>a2.foo7(JavaBox(null)).a<!>
}