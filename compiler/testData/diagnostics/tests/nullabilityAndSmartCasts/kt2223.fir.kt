//KT-2223 Comparing non-null value with null might produce helpful warning
package kt2223

fun foo() {
    val x: Int? = null
    if (x == null) return
    if (x == null) return
}
