// !DIAGNOSTICS: -JAVA_LANG_CLASS_PARAMETER_IN_ANNOTATION
package a.b.c

kotlin.deprecated("aaa")
ann1(kotlin.deprecated("aaa"))

a.b.c.ann1()
ann2(a.b.c.ann1())

A.IAnn()
ann3(A.IAnn())

a.b.c.A.IAnn()
ann3(a.b.c.A.IAnn())

annJavaClass(kotlin.javaClass<A>())

annArray(kotlin.arrayOf("a"))
fun test() = 1

annotation class ann1(val p: deprecated = kotlin.deprecated("aaa"))
annotation class ann2(val p: ann1 = a.b.c.ann1())
annotation class ann3(val p: A.IAnn = A.IAnn(), val p2: A.IAnn = a.b.c.A.IAnn())

annotation class annJavaClass(val p: Class<*> = kotlin.javaClass<A>())

annotation class annArray(val p: Array<String> = kotlin.arrayOf("a"))

class A {
    annotation class IAnn
}