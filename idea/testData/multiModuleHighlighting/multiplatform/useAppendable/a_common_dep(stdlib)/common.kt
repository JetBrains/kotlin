// KT-20824
package foo

fun <O : Appendable> O.appendMe(): O = this // kotlin.text.Appendable
