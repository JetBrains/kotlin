// KT-55828
// IGNORE_BACKEND_K2: NATIVE
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
