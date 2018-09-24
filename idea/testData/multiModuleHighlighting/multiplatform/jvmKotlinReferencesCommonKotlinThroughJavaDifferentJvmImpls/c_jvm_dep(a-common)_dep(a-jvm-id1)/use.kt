package c

import common.A

fun use(a: A) {
    // errors happen when common module part appears before platform part in dependencies list
    // this is incorrect behaviour but doesn't seem to lead to user facing bugs atm
    a.<error>id1</error>()
    val a2: A = j.Use.returnA()
    a2.<error>id1</error>()
}