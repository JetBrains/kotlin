// IGNORE_REVERSED_RESOLVE
// !DIAGNOSTICS: -UNUSED_VARIABLE, -UNUSED_PARAMETER
class A

@Target(AnnotationTarget.TYPE)
annotation class x

fun @x A.foo(a: @x Int) {
    val v: @x Int = 1
}

fun <T> @x List<@x T>.foo(l: List<@x T>): @x List<@x T> = throw Exception()

fun <T, U: T> List<@x T>.firstTyped(): U = throw Exception()

val <T> @x List<@x T>.f: Int get() = 42

@Target(AnnotationTarget.TYPE)
annotation class TypeAnnWithArg(val arg: String)

fun badArgs(a: List<@TypeAnnWithArg(<!NO_VALUE_FOR_PARAMETER!><!NAMED_PARAMETER_NOT_FOUND!>unresolved<!> = "")<!> Int>) {}
fun badArgsWithProjection(a: Array<out @TypeAnnWithArg(<!NO_VALUE_FOR_PARAMETER!><!NAMED_PARAMETER_NOT_FOUND!>unresolved<!> = "")<!> Int>) {}

typealias BadArgsInTypeAlias = List<@<!NO_VALUE_FOR_PARAMETER!>TypeAnnWithArg<!> Int>
fun badArgsInTypeAlias(a: BadArgsInTypeAlias) {}

typealias T<X> = List<X>
fun badArgsInTypeAliasInstance(a: T<@TypeAnnWithArg(arg = <!CONSTANT_EXPECTED_TYPE_MISMATCH!>123<!>) Int>) {}

typealias BadArgsInTypeParameter<<!WRONG_ANNOTATION_TARGET!>@TypeAnnWithArg(arg = <!CONSTANT_EXPECTED_TYPE_MISMATCH!>123<!>)<!> X> = List<X>

typealias BadArgsInRecursive = List<Map<List<@<!NO_VALUE_FOR_PARAMETER!>TypeAnnWithArg<!> Int>, @<!NO_VALUE_FOR_PARAMETER!>TypeAnnWithArg<!> String>>

typealias BadArgsMultiple = Map<@<!NO_VALUE_FOR_PARAMETER!>TypeAnnWithArg<!> Int, @TypeAnnWithArg(arg = <!CONSTANT_EXPECTED_TYPE_MISMATCH!>123<!>) Int>
