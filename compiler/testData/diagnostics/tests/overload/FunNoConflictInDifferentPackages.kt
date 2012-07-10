// FILE: ns1.kt
// check no error in overload in different namespaces

package ns1
fun e() = 1

// FILE: ns2.kt
package ns2
fun e() = 1

// FILE: ns3ns1.kt
package ns3.ns1
fun e() = 1
