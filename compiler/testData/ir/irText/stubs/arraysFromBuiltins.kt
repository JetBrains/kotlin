// DUMP_EXTERNAL_CLASS: kotlin/Array
// DUMP_EXTERNAL_CLASS: kotlin/IntArray
// DUMP_EXTERNAL_CLASS: kotlin/collections/IntIterator
// TARGET_BACKEND: JVM
// DUMP_IR_DIFFERENCE: JKLIB
// ^ JKlib tests compile against JDK 17 instead of the mock JDK.

val test = IntArray(1).iterator()
