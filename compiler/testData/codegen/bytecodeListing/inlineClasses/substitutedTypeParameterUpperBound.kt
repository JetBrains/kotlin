// TARGET_BACKEND: JVM
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +JvmInlineMultiFieldValueClasses
// WITH_SIGNATURES

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z1<T>(val value: T) where T : CharSequence, T : Comparable<String>

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z2<T>(val value: T) where T : Comparable<String>, T : CharSequence

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z3<T>(val value: Array<T>) where T : CharSequence, T : Comparable<String>

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z4<T>(val value: Array<T>) where T : Comparable<String>, T : CharSequence

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z5<T, U : T, V : U>(val value: V)

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z6<T : CharSequence>(val value: Array<in T>)

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z7<T : List<F>, F>(val value: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z8<F>(val value: List<F>)

fun f1(): Z1<String>? = null
fun f2(): Z2<String>? = null
fun f3(): Z3<String>? = null
fun f4(): Z4<String>? = null
fun f5(): Z5<Any, CharSequence, String>? = null
fun f6(): Z6<String>? = null
fun f7a(): Z7<*, String>? = null
fun <T: List<String>> f7b(): Z7<T, String>? = null
fun f8a(): Z8<String>? = null
fun <T> f8b(): Z8<T>? = null

fun g1(z: Z1<String>): String = z.value
fun g2(z: Z2<String>): String = z.value
fun g3(z: Z3<String>): String = z.value.first()
fun g4(z: Z3<String>): String = z.value.first()
fun g5(z: Z5<Any, CharSequence, String>): String = z.value
fun g6(z: Z6<String>): Any? = z.value.first()
fun g7a(z: Z7<*, String>): Any? = z.value.first()
fun <T: List<String>> g7b(z: Z7<T, String>): String = z.value.first()
fun g8a(z: Z8<String>): String = z.value.first()
fun <T> g8b(z: Z8<T>): T = z.value.first()
