// FIR_COMPARISON
interface Expr {
    public fun testThis() {
        if (this is Num) {
            this.<caret>
        }
    }
}

class Num(val toCheck : Int) : Expr

// EXIST: toCheck