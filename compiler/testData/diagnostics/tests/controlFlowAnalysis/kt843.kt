//KT-843 Don't highlight incomplete variables as unused

package kt843

fun main() {
    // Integer type
    val<!SYNTAX!><!> // this word is grey, which looks strange
}