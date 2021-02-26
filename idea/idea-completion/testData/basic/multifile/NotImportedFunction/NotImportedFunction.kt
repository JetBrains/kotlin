// FIR_COMPARISON
package pack

fun testFun() {
    gl<caret>
}

// EXIST: globalFun1, globalFun2
// ABSENT: globalFun3
