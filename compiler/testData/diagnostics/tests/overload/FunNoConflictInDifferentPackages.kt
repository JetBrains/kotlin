// FILE: pkg1.kt
// check no error in overload in different packages

package pkg1
fun e() = 1

// FILE: pkg2.kt
package pkg2
fun e() = 1

// FILE: pkg3pkg1.kt
package pkg3.pkg1
fun e() = 1
