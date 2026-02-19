// FILE: main.kt
val pp = 10

/**
 * [p<caret>p.bar]
 */
fun foo() {}

// FILE: pp/dependent.kt
package pp

fun bar(){}