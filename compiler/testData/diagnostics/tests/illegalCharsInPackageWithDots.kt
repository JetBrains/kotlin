// ISSUE: KT-69475

// FILE: A.kt
package nest.<!INVALID_CHARACTERS!>`a bc.d e`<!>.vest

fun test() {}

// FILE: B.kt
package main

import nest.`a bc`.`d e`.vest.*

fun main() = test()
