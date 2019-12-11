class C {

    fun f (a : Boolean, b : Boolean) {
        b@ while (true)
          a@ {
            break@f
            break
            break@b
            break@a
          }

        continue

        b@ while (true)
          a@ {
            continue@f
            continue
            continue@b
            continue@a
          }

        break

        continue@f
        break@f
    }

    fun containsBreak(a: String?, b: String?) {
        while (a == null) {
            break;
        }
        a.<!INAPPLICABLE_CANDIDATE!>compareTo<!>("2")
    }

    fun notContainsBreak(a: String?, b: String?) {
        while (a == null) {
            while (b == null) {
                break;
            }
        }
        a.<!INAPPLICABLE_CANDIDATE!>compareTo<!>("2")
    }

    fun containsBreakWithLabel(a: String?) {
        loop@ while(a == null) {
            break@loop
        }
        a?.compareTo("2")
    }

    fun containsIllegalBreak(a: String?) {
        loop@ while(a == null) {
            break@label
        }
        a.<!INAPPLICABLE_CANDIDATE!>compareTo<!>("2")
    }

    fun containsBreakToOuterLoop(a: String?, b: String?) {
        loop@ while(b == null) {
            while(a == null) {
                break@loop
            }
            a.<!INAPPLICABLE_CANDIDATE!>compareTo<!>("2")
        }
    }

    fun containsBreakInsideLoopWithLabel(a: String?, array: Array<Int>) {
        l@ while(a == null) {
            for (el in array) {
                break@l
            }
        }
        a.<!INAPPLICABLE_CANDIDATE!>compareTo<!>("2")
    }

    fun unresolvedBreak(a: String?, array: Array<Int>) {
        while(a == null) {
            l@ for (el in array) {
                break
            }
            if (true) break else break@l
        }
        a.<!INAPPLICABLE_CANDIDATE!>compareTo<!>("2")
    }

    fun twoLabelsOnLoop() {
        label1@ label2@ for (i in 1..100) {
            if (i > 0) {
                break@label1
            }
            else {
                break@label2
            }
        }
    }
}
