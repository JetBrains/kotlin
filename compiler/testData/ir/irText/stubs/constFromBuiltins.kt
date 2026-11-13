// DUMP_EXTERNAL_CLASS: kotlin/Int
// TARGET_BACKEND: JVM
// DUMP_IR_DIFFERENCE: JKLIB
// ^ JKlib tests compile against JDK 17 instead of the mock JDK.
// ^ JKlib reads the builtins from the stdlib KLIB.

val test = Int.MIN_VALUE
