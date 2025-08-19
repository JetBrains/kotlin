package com
/** [com.A<caret_1>A] - to class AA */
class AA
/** [com.A<caret_2>A] - to class AA */
fun AA(a: Int) {}

/**
 * [com.A<caret_3>A] leads to the class AA
 * since the priority of a class is higher than a function
 *
 * [co<caret_4>m] - package
 */
 fun usage() = 0