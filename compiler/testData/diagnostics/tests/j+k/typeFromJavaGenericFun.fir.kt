// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -DontMakeExplicitJavaTypeArgumentsFlexible -PreciseSimplificationToFlexibleLowerConstraint

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
    takeAny(JavaWithGenericFun.<!CANNOT_INFER_PARAMETER_TYPE!>foo<!>(JavaBox("")).a)
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
    takeString(<!ARGUMENT_TYPE_MISMATCH!>JavaWithGenericFun.foo3(JavaBox(null)).a<!>)
    takeString(JavaWithGenericFun.foo3<String>(JavaBox(null)).a)
    takeString(JavaWithGenericFun.foo3<String?>(JavaBox(null)).a)

    takeString(JavaWithGenericFun.foo4(JavaBox(JavaBox(""))).a.a)
    takeString(JavaWithGenericFun.foo4<String>(JavaBox(JavaBox(""))).a.a)
    takeString(<!NULLABILITY_MISMATCH_BASED_ON_EXPLICIT_TYPE_ARGUMENTS_FOR_JAVA("String?; String;  This will become an error in a future release. See https://youtrack.jetbrains.com/issue/KTLC-284.")!>JavaWithGenericFun.foo4<String?>(JavaBox(JavaBox(""))).a.a<!>)
    takeString(<!ARGUMENT_TYPE_MISMATCH!>JavaWithGenericFun.foo4(JavaBox(JavaBox(null))).a.a<!>)
    takeString(JavaWithGenericFun.foo4<String>(JavaBox(JavaBox(null))).a.a)
    takeString(<!NULLABILITY_MISMATCH_BASED_ON_EXPLICIT_TYPE_ARGUMENTS_FOR_JAVA("String?; String;  This will become an error in a future release. See https://youtrack.jetbrains.com/issue/KTLC-284.")!>JavaWithGenericFun.foo4<String?>(JavaBox(JavaBox(null))).a.a<!>)

    takeAny(JavaWithGenericFun.foo5(JavaBox(JavaBox(""))).a)
    takeAny(JavaWithGenericFun.foo5<String>(JavaBox(JavaBox(""))).a)
    takeAny(JavaWithGenericFun.foo5<String?>(JavaBox(JavaBox(""))).a)
    takeAny(JavaWithGenericFun.foo5(JavaBox(JavaBox(null))).a)
    takeAny(JavaWithGenericFun.foo5<String>(JavaBox(JavaBox(null))).a)
    takeAny(JavaWithGenericFun.foo5<String?>(JavaBox(JavaBox(null))).a)

    takeString(JavaWithGenericFun.foo6(JavaBox("")).a)
    takeString(JavaWithGenericFun.foo6<String>(JavaBox("")).a)
    takeString(<!NULLABILITY_MISMATCH_BASED_ON_EXPLICIT_TYPE_ARGUMENTS_FOR_JAVA("String?; String;  This will become an error in a future release. See https://youtrack.jetbrains.com/issue/KTLC-284.")!>JavaWithGenericFun.foo6<String?>(JavaBox("")).a<!>)
    takeString(<!ARGUMENT_TYPE_MISMATCH!>JavaWithGenericFun.foo6(JavaBox(null)).a<!>)
    takeString(JavaWithGenericFun.foo6<String>(JavaBox(null)).a)
    takeString(<!NULLABILITY_MISMATCH_BASED_ON_EXPLICIT_TYPE_ARGUMENTS_FOR_JAVA("String?; String;  This will become an error in a future release. See https://youtrack.jetbrains.com/issue/KTLC-284.")!>JavaWithGenericFun.foo6<String?>(JavaBox(null)).a<!>)
}

fun takeString(a: String){}
fun takeAny(a: Any){}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, inProjection, integerLiteral, javaFunction, javaType,
nullableType, outProjection, starProjection, stringLiteral */
