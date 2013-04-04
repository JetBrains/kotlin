class C {

    fun f (<!UNUSED_PARAMETER!>a<!> : Boolean, <!UNUSED_PARAMETER!>b<!> : Boolean) {
        @b <!USELESS_PARENTHESES!>(while (true)
          @a {
            <!NOT_A_LOOP_LABEL!>break@f<!>
            break
            break@b
            <!NOT_A_LOOP_LABEL!>break@a<!>
        })<!>

        <!BREAK_OR_CONTINUE_OUTSIDE_A_LOOP!>continue<!>

        @b <!USELESS_PARENTHESES!>(while (true)
          @a {
            <!NOT_A_LOOP_LABEL!>continue@f<!>
            continue
            continue@b
            <!NOT_A_LOOP_LABEL!>continue@a<!>
        })<!>

        <!BREAK_OR_CONTINUE_OUTSIDE_A_LOOP!>break<!>

        <!NOT_A_LOOP_LABEL!>continue@f<!>
        <!NOT_A_LOOP_LABEL!>break@f<!>
    }

    fun containsBreak(a: String?, <!UNUSED_PARAMETER!>b<!>: String?) {
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
        @loop while(a == null) {
            break@loop
        }
        a?.compareTo("2")
    }

    fun containsIllegalBreak(a: String?) {
        @loop while(a == null) {
            <!NOT_A_LOOP_LABEL!>break<!UNRESOLVED_REFERENCE!>@label<!><!>
        }
        a.compareTo("2")
    }

    fun containsBreakToOuterLoop(a: String?, b: String?) {
        @loop while(b == null) {
            while(a == null) {
                break@loop
            }
            a.compareTo("2")
        }
    }

    fun containsBreakInsideLoopWithLabel(a: String?, array: Array<Int>) {
        @ while(a == null) {
            for (el in array) {
                break@
            }
        }
        a<!UNSAFE_CALL!>.<!>compareTo("2")
    }

    fun unresolvedBreak(a: String?, array: Array<Int>) {
        while(a == null) {
            @ for (el in array) {
                break
            }
            if (true) break else <!NOT_A_LOOP_LABEL!>break<!UNRESOLVED_REFERENCE!>@<!><!>
        }
        a<!UNSAFE_CALL!>.<!>compareTo("2")
    }
}
