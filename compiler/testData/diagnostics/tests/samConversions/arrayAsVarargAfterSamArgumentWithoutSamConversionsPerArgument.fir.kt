// DIAGNOSTICS: -UNUSED_VARIABLE
// LANGUAGE: +SamConversionForKotlinFunctions -ProhibitVarargAsArrayAfterSamArgument -SamConversionPerArgument
// IGNORE_BACKEND: JS, JS_IR
// SKIP_TXT

// FILE: Test.java
public class Test {
    public static String foo1(Runnable r, String... strs) {
        return null;
    }
    public String foo2(Runnable r, Runnable r, String... strs) {
        return null;
    }
    public Test(Runnable r, String... strs) {}
    public Test(Runnable r1, Runnable r2, String... strs) {}
}

// FILE: main.kt
fun main(x2: Runnable) {
    val x1 = {}
    val x3 = arrayOf<String>()

    Test.foo1({}, <!ARGUMENT_TYPE_MISMATCH!><!CANNOT_INFER_PARAMETER_TYPE!>arrayOf<!>()<!>)
    Test.foo1({}, *arrayOf())
    Test.foo1({}, *x3)
    Test.foo1({}, <!ARGUMENT_TYPE_MISMATCH!>arrayOf("")<!>)

    Test.foo1(x1, <!ARGUMENT_TYPE_MISMATCH!><!CANNOT_INFER_PARAMETER_TYPE!>arrayOf<!>()<!>)
    Test.foo1(x1, *arrayOf())
    Test.foo1(x2, <!ARGUMENT_TYPE_MISMATCH!><!CANNOT_INFER_PARAMETER_TYPE!>arrayOf<!>()<!>)
    Test.foo1(x2, *arrayOf())

    Test.foo1(x1, <!ARGUMENT_TYPE_MISMATCH!>x3<!>)
    Test.foo1(x1, *x3)
    Test.foo1(x2, <!ARGUMENT_TYPE_MISMATCH!>arrayOf("")<!>)
    Test.foo1(x2, *arrayOf(""))

    val i1 = <!NONE_APPLICABLE!>Test<!>({}, <!CANNOT_INFER_PARAMETER_TYPE!>arrayOf<!>())
    val i2 = Test({}, *arrayOf())
    val i3 = <!NONE_APPLICABLE!>Test<!>({}, x3)
    val i4 = <!NONE_APPLICABLE!>Test<!>({}, arrayOf(""))
    val i5 = Test({}, {}, *arrayOf(""))
    val i6 = <!NONE_APPLICABLE!>Test<!>({}, {}, <!CANNOT_INFER_PARAMETER_TYPE!>arrayOf<!>())

    i1.<!UNRESOLVED_REFERENCE!>foo2<!>({}, {}, <!CANNOT_INFER_PARAMETER_TYPE!>arrayOf<!>())
    i1.<!UNRESOLVED_REFERENCE!>foo2<!>({}, {}, *<!CANNOT_INFER_PARAMETER_TYPE!>arrayOf<!>())
    i1.<!UNRESOLVED_REFERENCE!>foo2<!>({}, x2, <!CANNOT_INFER_PARAMETER_TYPE!>arrayOf<!>())
    i1.<!UNRESOLVED_REFERENCE!>foo2<!>(x2, {}, *<!CANNOT_INFER_PARAMETER_TYPE!>arrayOf<!>())

    i1.<!UNRESOLVED_REFERENCE!>foo2<!>({}, {}, arrayOf(""))
    i1.<!UNRESOLVED_REFERENCE!>foo2<!>({}, {}, *x3)
    i1.<!UNRESOLVED_REFERENCE!>foo2<!>({}, x2, x3)
    i1.<!UNRESOLVED_REFERENCE!>foo2<!>(x2, {}, *arrayOf(""))
}
