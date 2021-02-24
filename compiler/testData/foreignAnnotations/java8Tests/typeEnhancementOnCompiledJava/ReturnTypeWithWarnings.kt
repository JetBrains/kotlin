// !LANGUAGE: +ProhibitUsingNullableTypeParameterAgainstNotNullAnnotated
// !DIAGNOSTICS: -UNUSED_PARAMETER
// SKIP_TXT

fun <K> takeNotNullStringAndKNullable(x: ReturnTypeWithWarnings.A<String, K?>) {}
fun <K> takeNullableStringAndKNullable(x: ReturnTypeWithWarnings.A<String?, K?>) {}
fun <K: Any> takeNotNullStringAndNotNullK(x: ReturnTypeWithWarnings.A<String, K>) {}
fun <K: Any> takeNullableStringAndNotNullK(x: ReturnTypeWithWarnings.A<String?, K>) {}
fun takeNotNullString(x: String) {}

fun takeArrayOfNotNullString(x: Array<String>) {}
fun takeArrayOfNullableString(x: Array<String?>) {}
fun <K: Any> takeArrayOfNotNullK(x: Array<K>) {}
fun <K> takeArrayOfNullableK(x: Array<K?>) {}

// FILE: main.kt
fun <R> main(a: ReturnTypeWithWarnings<R>) {
    val x1 = <!DEBUG_INFO_EXPRESSION_TYPE("(ReturnTypeWithWarnings.A<(@org.jetbrains.annotations.Nullable kotlin.String..@org.jetbrains.annotations.Nullable kotlin.String?), (@org.jetbrains.annotations.Nullable R..@org.jetbrains.annotations.Nullable R?)>..ReturnTypeWithWarnings.A<(@org.jetbrains.annotations.Nullable kotlin.String..@org.jetbrains.annotations.Nullable kotlin.String?), (@org.jetbrains.annotations.Nullable R..@org.jetbrains.annotations.Nullable R?)>?)")!>a.foo1()<!>
    takeNotNullStringAndKNullable(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS("ReturnTypeWithWarnings.A<String, R?>", "ReturnTypeWithWarnings.A<String?, R?>!")!>x1<!>)
    takeNullableStringAndKNullable(x1)
    takeNotNullStringAndNotNullK(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS("ReturnTypeWithWarnings.A<String, R?>", "ReturnTypeWithWarnings.A<String?, R?>!"), TYPE_MISMATCH("Any", "R!"), TYPE_MISMATCH("Any", "R!")!>x1<!>)
    takeNullableStringAndNotNullK(<!TYPE_MISMATCH, TYPE_MISMATCH!>x1<!>)
    takeNotNullString(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS("String", "String?")!>a.foo41.foo411<!>)

    val x2 = <!DEBUG_INFO_EXPRESSION_TYPE("(ReturnTypeWithWarnings.A<(@org.jetbrains.annotations.Nullable kotlin.String..@org.jetbrains.annotations.Nullable kotlin.String?), (@org.jetbrains.annotations.NotNull R..@org.jetbrains.annotations.NotNull R?)>..ReturnTypeWithWarnings.A<(@org.jetbrains.annotations.Nullable kotlin.String..@org.jetbrains.annotations.Nullable kotlin.String?), (@org.jetbrains.annotations.NotNull R..@org.jetbrains.annotations.NotNull R?)>?)")!>a.foo2()<!>
    takeNotNullStringAndKNullable(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS("ReturnTypeWithWarnings.A<String, R?>", "ReturnTypeWithWarnings.A<String?, R!!>!")!>x2<!>)
    takeNullableStringAndKNullable(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS("ReturnTypeWithWarnings.A<String?, R?>", "ReturnTypeWithWarnings.A<String?, R!!>!")!>x2<!>)
    takeNotNullStringAndNotNullK(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS("ReturnTypeWithWarnings.A<String, R!!>", "ReturnTypeWithWarnings.A<String?, R!!>!"), TYPE_MISMATCH("Any", "R!"), TYPE_MISMATCH("Any", "R!")!>x2<!>)
    takeNullableStringAndNotNullK(<!TYPE_MISMATCH, TYPE_MISMATCH!>x2<!>)

    val x3 = <!DEBUG_INFO_EXPRESSION_TYPE("(ReturnTypeWithWarnings.A<(@org.jetbrains.annotations.NotNull kotlin.String..@org.jetbrains.annotations.NotNull kotlin.String?), (@org.jetbrains.annotations.NotNull R..@org.jetbrains.annotations.NotNull R?)>..ReturnTypeWithWarnings.A<(@org.jetbrains.annotations.NotNull kotlin.String..@org.jetbrains.annotations.NotNull kotlin.String?), (@org.jetbrains.annotations.NotNull R..@org.jetbrains.annotations.NotNull R?)>?)")!>a.foo3<!>
    takeNotNullStringAndKNullable(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS("ReturnTypeWithWarnings.A<String, R?>", "ReturnTypeWithWarnings.A<String, R!!>!")!>x3<!>)
    takeNullableStringAndKNullable(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS("ReturnTypeWithWarnings.A<String?, R?>", "ReturnTypeWithWarnings.A<String, R!!>!")!>x3<!>)
    takeNotNullStringAndNotNullK(<!TYPE_MISMATCH, TYPE_MISMATCH!>x3<!>)
    takeNullableStringAndNotNullK(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS("ReturnTypeWithWarnings.A<String?, R!!>", "ReturnTypeWithWarnings.A<String, R!!>!"), TYPE_MISMATCH("Any", "R!"), TYPE_MISMATCH("Any", "R!")!>x3<!>)

    val x4 = <!DEBUG_INFO_EXPRESSION_TYPE("(kotlin.Array<(@org.jetbrains.annotations.NotNull R..@org.jetbrains.annotations.NotNull R?)>..kotlin.Array<out (@org.jetbrains.annotations.NotNull R..@org.jetbrains.annotations.NotNull R?)>)")!>a.foo4<!>
    takeArrayOfNotNullString(<!TYPE_MISMATCH!>x4<!>)
    takeArrayOfNullableString(<!TYPE_MISMATCH!>x4<!>)
    takeArrayOfNotNullK(<!TYPE_MISMATCH, TYPE_MISMATCH!>x4<!>)
    takeArrayOfNullableK(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS("Array<R?>", "Array<(out) R!!>")!>x4<!>)

    val x5 = <!DEBUG_INFO_EXPRESSION_TYPE("(kotlin.Array<(@org.jetbrains.annotations.Nullable kotlin.String..@org.jetbrains.annotations.Nullable kotlin.String?)>?..kotlin.Array<out (@org.jetbrains.annotations.Nullable kotlin.String..@org.jetbrains.annotations.Nullable kotlin.String?)>?)")!>a.foo5()<!>
    takeArrayOfNotNullString(<!TYPE_MISMATCH!>x5<!>)
    takeArrayOfNullableString(<!TYPE_MISMATCH!>x5<!>)
    takeArrayOfNotNullK(<!TYPE_MISMATCH!>x5<!>)
    takeArrayOfNullableK(<!TYPE_MISMATCH!>x5<!>)
}
