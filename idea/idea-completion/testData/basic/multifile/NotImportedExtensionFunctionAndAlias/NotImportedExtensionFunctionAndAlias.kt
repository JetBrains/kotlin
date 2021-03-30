// FIR_COMPARISON
package first

fun firstFun(x: third.Dependency) {
    x.hello<caret>
}

// EXIST: helloFun
// EXIST: helloFunGeneric
// NOTHING_ELSE
