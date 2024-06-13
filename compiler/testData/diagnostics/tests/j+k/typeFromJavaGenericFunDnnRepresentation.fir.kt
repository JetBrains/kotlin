// LANGUAGE: +JavaTypeParameterDefaultRepresentationWithDNN

// FILE: JavaWithGenericFun.java
public class JavaWithGenericFun {
    public static <T> JavaBox<?> foo(JavaBox<?> a) { return null; }

    public static <T> JavaBox<? super T> foo2(JavaBox<? super T> a) { return null; }

    public static <T> JavaBox<? super T> foo2_2(T a) { return null; }

    public static <T> JavaBox<? extends T> foo3(JavaBox<? extends T> a) { return null; }

    public static <T> JavaBox<? extends JavaBox<T>> foo4(JavaBox<? extends JavaBox<T>> a) { return null; }

    public static <T> JavaBox<? super JavaBox<T>> foo5(JavaBox<? super JavaBox<T>> a) { return null; }

    public static <T> JavaBox<T> foo6(JavaBox<T> a) { return null; }
}

// FILE: JavaBox.java
public class JavaBox<T> {
    public JavaBox(T b) { a = b; }
    public T a;
}

// FILE: Test.kt
fun geneticFunTest() {
    takeAny(JavaWithGenericFun.<!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>(JavaBox("")).a)
    takeAny(JavaWithGenericFun.foo<String>(JavaBox("")).a)
    takeAny(JavaWithGenericFun.foo<String>(JavaBox(null)).a)
    takeAny(JavaWithGenericFun.foo<String?>(JavaBox(null)).a)
    takeAny(JavaWithGenericFun.foo<String?>(JavaBox("")).a)

    takeAny(JavaWithGenericFun.foo2(JavaBox(1)).a)
    takeAny(JavaWithGenericFun.foo2(JavaBox(null)).a)
    takeAny(JavaWithGenericFun.foo2<Int>(JavaBox(1)).a)
    takeAny(JavaWithGenericFun.foo2<Int?>(JavaBox(1)).a)

    takeString(JavaWithGenericFun.foo3(JavaBox("")).a)
    takeString(JavaWithGenericFun.foo3<String>(JavaBox("")).a)
    takeString(JavaWithGenericFun.foo3<String?>(JavaBox("")).a)
    takeString(JavaWithGenericFun.foo3(JavaBox(null)).a)
    takeString(JavaWithGenericFun.foo3<String>(JavaBox(null)).a)
    takeString(JavaWithGenericFun.foo3<String?>(JavaBox(null)).a)

    takeString(JavaWithGenericFun.foo4(JavaBox(JavaBox(""))).a.a)
    takeString(JavaWithGenericFun.foo4<String>(JavaBox(JavaBox(""))).a.a)
    takeString(JavaWithGenericFun.foo4<String?>(JavaBox(JavaBox(""))).a.a)
    takeString(JavaWithGenericFun.foo4(JavaBox(JavaBox(null))).a.a)
    takeString(JavaWithGenericFun.foo4<String>(JavaBox(JavaBox(null))).a.a)
    takeString(JavaWithGenericFun.foo4<String?>(JavaBox(JavaBox(null))).a.a)

    takeAny(JavaWithGenericFun.foo5(JavaBox(JavaBox(""))).a)
    takeAny(JavaWithGenericFun.foo5<String>(JavaBox(JavaBox(""))).a)
    takeAny(JavaWithGenericFun.foo5<String?>(JavaBox(JavaBox(""))).a)
    takeAny(JavaWithGenericFun.foo5(JavaBox(JavaBox(null))).a)
    takeAny(JavaWithGenericFun.foo5<String>(JavaBox(JavaBox(null))).a)
    takeAny(JavaWithGenericFun.foo5<String?>(JavaBox(JavaBox(null))).a)

    takeString(JavaWithGenericFun.foo6(JavaBox("")).a)
    takeString(JavaWithGenericFun.foo6<String>(JavaBox("")).a)
    takeString(JavaWithGenericFun.foo6<String?>(JavaBox("")).a)
    takeString(JavaWithGenericFun.foo6(JavaBox(null)).a)
    takeString(JavaWithGenericFun.foo6<String>(JavaBox(null)).a)
    takeString(JavaWithGenericFun.foo6<String?>(JavaBox(null)).a)
}

fun takeString(a: String){}
fun takeAny(a: Any){}
