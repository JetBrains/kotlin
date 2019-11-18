// IGNORE_BACKEND_FIR: JVM_IR
package test

enum class Season {
    WINTER,
    SPRING,
    SUMMER,
    AUTUMN
}

fun foo(): Season = Season.SPRING

fun box() =
    if (foo() == Season.SPRING) "OK"
    else "fail"
