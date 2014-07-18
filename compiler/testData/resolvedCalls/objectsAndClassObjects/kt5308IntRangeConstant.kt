//KT-5308 Wrong ExplicitReceiverKind when access to IntRange.EMPTY
fun test() {
    IntRange.<caret>EMPTY
}