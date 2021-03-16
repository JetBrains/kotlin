// !DIAGNOSTICS: -DUPLICATE_CLASS_NAMES
<error descr="[REDECLARATION] Conflicting declarations: [/a]">val a : Int = 1</error>
<error descr="[REDECLARATION] Conflicting declarations: [/a]">val a : Int = 1</error>

<error descr="[CONFLICTING_OVERLOADS] Conflicting overloads: [/foo]">fun foo()</error> {}
<error descr="[CONFLICTING_OVERLOADS] Conflicting overloads: [/foo]">fun foo()</error> {}

enum class EnumClass {
    FOO,
    FOO
}

class A {
    <error descr="[REDECLARATION] Conflicting declarations: [/A.a]">val a : Int = 1</error>
    <error descr="[REDECLARATION] Conflicting declarations: [/A.a]">val a : Int = 1</error>

    <error descr="[CONFLICTING_OVERLOADS] Conflicting overloads: [/A.foo]">fun foo()</error> {}
    <error descr="[CONFLICTING_OVERLOADS] Conflicting overloads: [/A.foo]">fun foo()</error> {}
}

object B {
    <error descr="[REDECLARATION] Conflicting declarations: [B.C]">class C</error>
    <error descr="[REDECLARATION] Conflicting declarations: [B.C]">class C</error>
}

fun <T, T> PairParam() {}
class PParam<T, T> {}

val <T, T> T.fooParam : Int get() = 1
