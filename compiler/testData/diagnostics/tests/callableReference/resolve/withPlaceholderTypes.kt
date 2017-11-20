// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER,-CONFLICTING_JVM_DECLARATIONS

fun foo(i: Int) = "$i"
fun foo(s: String) = s

fun bar(s: String) = s

fun qux(i: Int, j: Int, k: Int): Int = i + j + k
fun qux(a: String, b: String, c: String, d: String) {}

fun fn1(x: Int, f1: (Int) -> String, f2: (String) -> String) = f2(f1(x))

fun fn2(f1: (Int) -> String,    f2: (String) -> String  ) = f2(f1(0))
fun fn2(f1: (Int) -> Int,       f2: (Int) -> String     ) = f2(f1(0))
fun fn2(f1: (String) -> String, f2: (String) -> String  ) = f2(f1(""))

fun fn3(i: Int, f: (Int, Int, Int) -> Int): Int = f(i, i, i)

val x1 = fn1(1, ::foo, ::foo)
val x2 = fn1(1, ::foo, ::bar)

val x3 = fn2(::bar, ::foo)
val x4 = <!OVERLOAD_RESOLUTION_AMBIGUITY!>fn2<!>(::<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>, ::bar)
val x5 = <!OVERLOAD_RESOLUTION_AMBIGUITY!>fn2<!>(::<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>, ::<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>)

val x6 = fn3(1, ::qux)