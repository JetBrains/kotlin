// LANGUAGE: +JavaTypeParameterDefaultRepresentationWithDNN

// FILE: GenericFunWithAnnotation.java
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GenericFunWithAnnotation {

    public <T> void foo(@NotNull T a) {}

    public <T> @NotNull T bar() { return null; }

    public <T> void foo2(@Nullable T a) {}

    public <T> @Nullable T bar2() { return null; }

    public <T> void foo3(@Nullable JavaBox<T> a) {}

    public <T> @Nullable JavaBox<T> bar3() { return null; }

    public <T> void foo4(@NotNull JavaBox<T> a) {}

    public <T> @NotNull JavaBox<T> bar4() { return null; }

    public <T> void foo5(@Nullable JavaBox<@Nullable T> a) {}

    public <T> @Nullable JavaBox<@Nullable T> bar5() { return null; }

    public <T> void foo6(@NotNull JavaBox<@NotNull T> a) {}

    public <T> @NotNull JavaBox<@NotNull T> bar6() { return null; }

    public <T extends @NotNull Object> void foo7(T a) {}

    public <T extends @NotNull Object> T bar7() { return null; }

    public <T extends @Nullable Object> void foo8(T a) {}

    public <T extends @Nullable Object> T bar8() { return null; }

    public <T extends @Nullable Object> void foo9(JavaBox<T> a) {}

    public <T extends @Nullable Object> JavaBox<T> bar9() { return null; }

    public <T extends @NotNull Object> void foo10(JavaBox<T> a) {}

    public <T extends @NotNull Object> JavaBox<T> bar10() { return null; }
}

// FILE: JavaBox.java
public class JavaBox<T> {
    public JavaBox(T b) {
        a = b;
    }
    public T a;
}

// FILE: Test.kt
fun genericFunWithAnnotations(x: GenericFunWithAnnotation) {
    x.foo("")
    x.<!CANNOT_INFER_PARAMETER_TYPE!>foo<!>(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    takeString(x.bar())

    x.foo2("")
    x.foo2(null)
    takeString(<!ARGUMENT_TYPE_MISMATCH!>x.<!CANNOT_INFER_PARAMETER_TYPE!>bar2<!>()<!>)

    x.<!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo3<!>(null)
    x.foo3<String>(null)
    x.foo3<String?>(null)
    x.foo3(JavaBox(null))
    x.foo3<String>(JavaBox(null))
    x.foo3<String?>(JavaBox(null))
    x.foo3(JavaBox(""))
    takeString(x.<!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>bar3<!>()?.a)

    x.foo4(JavaBox(null))
    x.foo4<String>(JavaBox(null))
    x.foo4<String?>(JavaBox(null))
    x.<!CANNOT_INFER_PARAMETER_TYPE!>foo4<!>(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    x.foo4<String>(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    x.foo4<String?>(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    x.foo4(JavaBox(""))
    takeString(x.<!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>bar4<!>().a)

    x.<!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo5<!>(null)
    x.foo5<String>(null)
    x.foo5<String?>(null)
    x.foo5(JavaBox(null))
    x.foo5(JavaBox(""))
    x.foo5<String>(JavaBox(null))
    x.foo5<String?>(JavaBox(null))
    takeString(x.<!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>bar5<!>()?.a)

    x.foo6(JavaBox(null))
    x.foo6<String>(JavaBox(null))
    x.foo6<String?>(JavaBox(null))
    x.<!CANNOT_INFER_PARAMETER_TYPE!>foo6<!>(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    x.foo6<String>(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    x.foo6<String?>(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    x.foo6(JavaBox(""))
    takeString(x.<!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>bar6<!>().a)

    x.foo7("")
    x.<!CANNOT_INFER_PARAMETER_TYPE!>foo7<!>(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    takeString(x.bar7())

    x.foo8("")
    x.foo8(null)
    takeString(x.bar8())

    x.<!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo9<!>(null)
    x.foo9<String>(null)
    x.foo9<String?>(null)
    x.foo9(JavaBox(null))
    x.foo9<String>(JavaBox(null))
    x.foo9<String?>(JavaBox(null))
    x.foo9(JavaBox(""))
    takeString(x.<!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>bar9<!>()?.a)

    x.foo10(JavaBox(null))
    x.foo10<String>(JavaBox(null))
    x.foo10<<!UPPER_BOUND_VIOLATED!>String?<!>>(<!ARGUMENT_TYPE_MISMATCH!>JavaBox(null)<!>)
    x.<!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo10<!>(null)
    x.foo10<String>(null)
    x.foo10<<!UPPER_BOUND_VIOLATED!>String?<!>>(<!ARGUMENT_TYPE_MISMATCH!>null<!>)
    x.foo10(JavaBox(""))
    takeString(x.<!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>bar10<!>().a)
}

fun takeString(a: String){}
