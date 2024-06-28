package test

interface Base

interface Child : Base

fun <T> T.anyExt() {}

fun <T : Base> T.baseExt() {}

fun <T : Child> T.childExt() {}

/**
 * [Any.<caret_1>anyExt]
 * [Base.<caret_2>anyExt]
 * [Child.<caret_3>anyExt]
 *
 * [Any.<caret_4>baseExt]
 * [Base.<caret_5>baseExt]
 * [Child.<caret_6>baseExt]
 *
 * [Any.<caret_7>childExt]
 * [Base.<caret_8>childExt]
 * [Child.<caret_9>childExt]
 */
fun usage() {}