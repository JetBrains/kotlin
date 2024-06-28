// ISSUE: KT-32754

import java.lang.invoke.MethodHandles

fun <T> Collection<T>.toArray(f: (Int) -> Array<T?>) = 1

fun String.<!EXTENSION_SHADOWED_BY_MEMBER!>strip<!>() = 1
fun String.<!EXTENSION_SHADOWED_BY_MEMBER!>stripLeading<!>() = 1
fun String.<!EXTENSION_SHADOWED_BY_MEMBER!>stripTrailing<!>() = 1
fun String.<!EXTENSION_SHADOWED_BY_MEMBER!>indent<!>(n: Int) = 1
fun String.<!EXTENSION_SHADOWED_BY_MEMBER!>stripIndent<!>() = 1
fun String.<!EXTENSION_SHADOWED_BY_MEMBER!>translateEscapes<!>() = 1
fun String.transform(f: (String) -> String) = 1
fun String.<!EXTENSION_SHADOWED_BY_MEMBER!>formatted<!>(vararg args: Any) = 1
fun String.repeat(n: Int) = 1
fun String.<!EXTENSION_SHADOWED_BY_MEMBER!>describeConstable<!>() = 1
fun String.<!EXTENSION_SHADOWED_BY_MEMBER!>resolveConstantDesc<!>(lookup: MethodHandles.Lookup) = 1

fun <E : Enum<E>> Enum<E>.<!EXTENSION_SHADOWED_BY_MEMBER!>describeConstable<!>() = 1

enum class MyEnum { E }

fun test(c: Collection<String>, l: List<String>, s: Set<String>) {
    consumeInt(<!TYPE_MISMATCH!>c.<!DEPRECATION, JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>toArray<!> { i -> arrayOfNulls<String>(i) }<!>)
    consumeInt(<!TYPE_MISMATCH!>l.<!DEPRECATION!>toArray<!> { i -> arrayOfNulls(i) }<!>)
    consumeInt(<!TYPE_MISMATCH!>s.<!DEPRECATION!>toArray<!> { i -> arrayOfNulls(i) }<!>)

    consumeInt(<!TYPE_MISMATCH!>"".<!DEPRECATION, JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>strip<!>()<!>)
    consumeInt(<!TYPE_MISMATCH!>"".<!DEPRECATION, JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>stripLeading<!>()<!>)
    consumeInt(<!TYPE_MISMATCH!>"".<!DEPRECATION, JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>stripTrailing<!>()<!>)
    consumeInt(<!TYPE_MISMATCH!>"".<!DEPRECATION, JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>indent<!>(1)<!>)
    consumeInt(<!TYPE_MISMATCH!>"".<!DEPRECATION, JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>stripIndent<!>()<!>)
    consumeInt(<!TYPE_MISMATCH!>"".<!DEPRECATION, JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>translateEscapes<!>()<!>)
    consumeInt(<!TYPE_MISMATCH!>"".<!DEPRECATION, JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>transform<!> { it -> "" }<!>)
    consumeInt(<!TYPE_MISMATCH!>"".<!DEPRECATION, JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>formatted<!>()<!>)
    consumeInt("".repeat(1))
    consumeInt(<!TYPE_MISMATCH!>"".<!DEPRECATION, JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>describeConstable<!>()<!>)
    consumeInt(<!TYPE_MISMATCH!>"".<!DEPRECATION, JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>resolveConstantDesc<!>(null <!CAST_NEVER_SUCCEEDS!>as<!> MethodHandles.Lookup)<!>)

    consumeInt(<!TYPE_MISMATCH!>MyEnum.E.<!DEPRECATION!>describeConstable<!>()<!>)
}

fun consumeInt(i: Int) {}
