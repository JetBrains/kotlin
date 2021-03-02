// FIR_COMPARISON
package first

fun firstFun(p: () -> Unit) {
    p.hello<caret>
}

// EXIST: helloFun1
// EXIST: helloFun2
// EXIST: helloAny
// ABSENT: helloFun3
// ABSENT: helloFun4
// NOTHING_ELSE
