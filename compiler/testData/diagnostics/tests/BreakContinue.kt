class C {

    fun f (<!UNUSED_PARAMETER!>a<!> : Boolean, <!UNUSED_PARAMETER!>b<!> : Boolean) {
        @b while (true)
          @a {
            <!NOT_A_LOOP_LABEL!>break@f<!>
            break
            <!UNREACHABLE_CODE!>break@b<!>
            <!NOT_A_LOOP_LABEL!>break@a<!>
          }

        <!BREAK_OR_CONTINUE_OUTSIDE_A_LOOP!>continue<!>

        @b while (true)
          @a {
            <!NOT_A_LOOP_LABEL!>continue@f<!>
            continue
            <!UNREACHABLE_CODE!>continue@b<!>
            <!NOT_A_LOOP_LABEL!>continue@a<!>
          }

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
        <!DEBUG_INFO_SMARTCAST!>a<!>.compareTo("2")
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
        <!DEBUG_INFO_SMARTCAST!>a<!>.compareTo("2")
    }

    fun containsBreakToOuterLoop(a: String?, b: String?) {
        @loop while(b == null) {
            while(a == null) {
                break@loop
            }
            <!DEBUG_INFO_SMARTCAST!>a<!>.compareTo("2")
        }
    }

    fun containsBreakInsideLoopWithLabel(a: String?, array: Array<Int>) {
        @l while(a == null) {
            for (el in array) {
                break@l
            }
        }
        a<!UNSAFE_CALL!>.<!>compareTo("2")
    }

    fun unresolvedBreak(a: String?, array: Array<Int>) {
        while(a == null) {
            @l for (el in array) {
                break
            }
            if (true) break else <!NOT_A_LOOP_LABEL!>break<!UNRESOLVED_REFERENCE!>@l<!><!>
        }
        a<!UNSAFE_CALL!>.<!>compareTo("2")
    }
}