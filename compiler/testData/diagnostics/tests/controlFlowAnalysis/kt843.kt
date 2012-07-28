//KT-843 Don't highlight incomplete variables as unused

package kt843

fun main(args : Array<String>) {
    // Integer type
    val<!SYNTAX!><!> // this word is grey, which looks strange
}