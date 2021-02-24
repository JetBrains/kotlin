// FIR_COMPARISON
package pack

class Boo

fun f() {
    x(fun (b<caret>))
}

// ABSENT: { itemText: "boo: Boo" }
