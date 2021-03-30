// FIR_COMPARISON
package ppp

fun f() {
    C().ext<caret>
}

// EXIST: extFun1
// EXIST: extFun2
// NOTHING_ELSE
