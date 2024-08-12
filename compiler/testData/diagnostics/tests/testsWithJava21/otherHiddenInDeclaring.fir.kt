// ISSUE: KT-32754

import java.lang.invoke.MethodHandles

fun <T> Collection<T>.toArray(f: (Int) -> Array<T?>) = 1

fun String.strip() = 1
fun String.stripLeading() = 1
fun String.stripTrailing() = 1
fun String.indent(n: Int) = 1
fun String.stripIndent() = 1
fun String.translateEscapes() = 1
fun String.transform(f: (String) -> String) = 1
fun String.formatted(vararg args: Any) = 1
fun String.repeat(n: Int) = 1
fun String.describeConstable() = 1
fun String.resolveConstantDesc(lookup: MethodHandles.Lookup) = 1

fun <E : Enum<E>> Enum<E>.<!EXTENSION_SHADOWED_BY_MEMBER!>describeConstable<!>() = 1

enum class MyEnum { E }

fun test(c: Collection<String>, l: List<String>, s: Set<String>) {
    consumeInt(c.toArray { i -> arrayOfNulls<String>(i) })
    consumeInt(l.toArray { i -> arrayOfNulls(i) })
    consumeInt(s.toArray { i -> arrayOfNulls(i) })

    consumeInt("".strip())
    consumeInt("".stripLeading())
    consumeInt("".stripTrailing())
    consumeInt("".indent(1))
    consumeInt("".stripIndent())
    consumeInt("".translateEscapes())
    consumeInt("".transform { it -> "" })
    consumeInt("".formatted())
    consumeInt("".repeat(1))
    consumeInt("".describeConstable())
    consumeInt("".resolveConstantDesc(null <!CAST_NEVER_SUCCEEDS!>as<!> MethodHandles.Lookup))

    consumeInt(MyEnum.E.describeConstable())
}

fun consumeInt(i: Int) {}