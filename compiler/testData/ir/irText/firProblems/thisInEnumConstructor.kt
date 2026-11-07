// WITH_STDLIB
// DUMP_IR_DIFFERENCE: JKLIB
// ^ JKlib tests compile against JDK 17 instead of the mock JDK.

enum class EE(val myName: String = this.toString().lowercase()) {
    ENTRY;
}
