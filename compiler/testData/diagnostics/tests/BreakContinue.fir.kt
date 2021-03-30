class C {

    fun f (a : Boolean, b : Boolean) {
        b@ while (true)
          a@ {
            <!NOT_A_LOOP_LABEL!>break@f<!>
            break
            break@b
            <!NOT_A_LOOP_LABEL!>break@a<!>
          }

        <!BREAK_OR_CONTINUE_OUTSIDE_A_LOOP!>continue<!>

        b@ while (true)
          a@ {
            <!NOT_A_LOOP_LABEL!>continue@f<!>
            continue
            continue@b
            <!NOT_A_LOOP_LABEL!>continue@a<!>
          }

        <!BREAK_OR_CONTINUE_OUTSIDE_A_LOOP!>break<!>

        <!BREAK_OR_CONTINUE_OUTSIDE_A_LOOP!>continue@f<!>
        <!BREAK_OR_CONTINUE_OUTSIDE_A_LOOP!>break@f<!>
    }

    fun containsBreak(a: String?, b: String?) {
        while (a == null) {
            break;
        }
        a<!UNSAFE_CALL!>.<!>compareTo("2")
    }

    fun notContainsBreak(a: String?, b: String?) {
        while (a == null) {
            while (b == null) {
                break;
            }
        }
        a.compareTo("2")
    }

    fun containsBreakWithLabel(a: String?) {
        loop@ while(a == null) {
            break@loop
        }
        a?.compareTo("2")
    }

    fun containsIllegalBreak(a: String?) {
        loop@ while(a == null) {
            <!NOT_A_LOOP_LABEL!>break@label<!>
        }
        a.compareTo("2")
    }

    fun containsBreakToOuterLoop(a: String?, b: String?) {
        loop@ while(b == null) {
            while(a == null) {
                break@loop
            }
            a.compareTo("2")
        }
    }

    fun containsBreakInsideLoopWithLabel(a: String?, array: Array<Int>) {
        l@ while(a == null) {
            for (el in array) {
                break@l
            }
        }
        a<!UNSAFE_CALL!>.<!>compareTo("2")
    }

    fun unresolvedBreak(a: String?, array: Array<Int>) {
        while(a == null) {
            l@ for (el in array) {
                break
            }
            if (true) break else <!NOT_A_LOOP_LABEL!>break@l<!>
        }
        a<!UNSAFE_CALL!>.<!>compareTo("2")
    }

    fun twoLabelsOnLoop() {
        label1@ label2@ for (i in 1..100) {
            if (i > 0) {
                <!NOT_A_LOOP_LABEL!>break@label1<!>
            }
            else {
                break@label2
            }
        }
    }
}
