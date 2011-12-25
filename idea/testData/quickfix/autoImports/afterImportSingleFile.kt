// "Import Class" "true"
package a

import a.b.M

fun test() {
    val v = M
}

package b {
    class M() { }
}