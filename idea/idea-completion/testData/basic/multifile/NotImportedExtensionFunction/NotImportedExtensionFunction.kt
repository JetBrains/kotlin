// FIR_COMPARISON
package first

fun firstFun() {
    val a = ""
    a.<caret>
}

// EXIST: helloFun
// EXIST: helloWithParams
// EXIST: helloFunGeneric
// EXIST: helloDynamic
// ABSENT: helloFake
