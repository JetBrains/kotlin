// "Import" "true"
// ERROR: Unresolved reference. None of the following candidates is applicable because of receiver type mismatch: <br>public operator fun A.unaryPlus(): Int defined in h in file unaryPlusOperator.before.Main.kt

package h

interface H

fun f(h: H?) {
    <caret>+h
}

class A()

operator fun A.unaryPlus(): Int = 3
/* IGNORE_FIR */