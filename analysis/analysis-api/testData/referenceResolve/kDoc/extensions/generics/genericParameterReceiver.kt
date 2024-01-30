package test

fun Any.anyExt() {}

fun <T> T.genericExt() {}

/**
 * [PARAM.<caret_1>anyExt]
 * [PARAM.<caret_2>genericExt]
 */
fun <PARAM> usage() {}

class Foo<CLASS_PARAM> {
    /**
     * [CLASS_PARAM.<caret_3>anyExt]
     * [CLASS_PARAM.<caret_4>genericExt]
     */
    fun nestedUsage() {}
}