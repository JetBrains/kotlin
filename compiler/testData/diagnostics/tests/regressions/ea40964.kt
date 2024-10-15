// RUN_PIPELINE_TILL: SOURCE
// FIR_IDENTICAL
package checkFiles

import java.util.HashMap

fun main() {
    val hashMap = HashMap<String, String>()
    hashMap[<!SYNTAX!><!>]
}
