// FIR_COMPARISON
package first

class C {
    fun String.firstFun() {
        hello<caret>
    }
}

// EXIST: helloFun
// EXIST: helloWithParams
// EXIST: helloProp
// EXIST: helloForC
// ABSENT: helloFake
// NOTHING_ELSE
