//KT-2212 Incomplete nullability information
package kt2212

fun main() {
    val x: Int? = 1
    if (x == null) return
    System.out.println(x.plus(x!!))
}
