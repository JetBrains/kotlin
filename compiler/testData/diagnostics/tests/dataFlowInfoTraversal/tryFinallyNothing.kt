fun foo1() : Int {
    <!UNREACHABLE_CODE!>val <!UNUSED_VARIABLE!>x<!>=<!>try {
    } finally {
        return 0
    }

<!UNREACHABLE_CODE!>x : Nothing<!>
}

fun foo2() : Int {
    <!UNREACHABLE_CODE!>val <!UNUSED_VARIABLE!>x<!>=<!>try {
    } finally {
        try {
            return 0
        } catch(e: Exception) {
            return 0
        }
    }

<!UNREACHABLE_CODE!>x : Nothing<!>
}

