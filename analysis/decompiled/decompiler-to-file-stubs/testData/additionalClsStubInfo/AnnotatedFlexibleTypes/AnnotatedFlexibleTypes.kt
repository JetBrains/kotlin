/* Java interop */
// KNM_K2_IGNORE

package test

public class AnnotatedFlexibleTypes(val javaClass: d.JavaClass) {
    fun foo() = javaClass.foo()

    val bar = javaClass.bar()

    val baz = javaClass.baz()
}