package test

class WithGenerics<T>

fun WithGenerics<Int>.intExt() {}

fun WithGenerics<String>.stringExt() {}

/**
 * [WithGenerics.<caret_1>intExt]
 * [WithGenerics.<caret_2>stringExt]
 */
fun usage() {}