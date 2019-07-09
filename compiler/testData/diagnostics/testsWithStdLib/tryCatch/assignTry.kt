// !WITH_NEW_INFERENCE
// SKIP_TXT

class ExcA : Exception()

class ExcB : Exception()

fun test2() {
    val s: String? = <!NI;TYPE_MISMATCH!>try {
        ""
    }
    catch (e: ExcA) {
        null
    }
    catch (e: ExcB) {
        <!OI;CONSTANT_EXPECTED_TYPE_MISMATCH!>10<!>
    }<!>
    s<!UNSAFE_CALL!>.<!>length
}

fun test3() {
    val s: String? = try {
        ""
    }
    catch (e: ExcA) {
        null
    }
    catch (e: ExcB) {
        return
    }
    s<!UNSAFE_CALL!>.<!>length
}

fun test4() {
    val s: String? = try {
        ""
    }
    catch (e: ExcA) {
        null
    }
    finally {
        <!UNUSED_EXPRESSION!>""<!>
    }
    s<!UNSAFE_CALL!>.<!>length
}

fun test5() {
    <!UNREACHABLE_CODE!>val <!UNUSED_VARIABLE!>s<!>: String? =<!> try {
        ""
    }
    catch (e: ExcA) {
        null
    }
    finally {
        return
    }
    <!UNREACHABLE_CODE!>s<!UNSAFE_CALL!>.<!>length<!>
}

fun test6() {
    val s: String? = try {
        ""
    }
    catch (e: ExcA) {
        return
    }
    catch (e: ExcB) {
        return
    }
    s<!UNSAFE_CALL!>.<!>length
}

fun test7() {
    val s: String? = try {
        ""
    }
    catch (e: ExcA) {
        ""
    }
    catch (e: ExcB) {
        ""
    }
    s<!UNSAFE_CALL!>.<!>length
}

fun test8() {
    val s = try {
        ""
    } catch (e: ExcA) {
        null
    }
    s<!UNSAFE_CALL!>.<!>length
}

fun test9() {
    val s = try {
        ""
    } catch (e: ExcA) {
        ""
    }
    s.length
}

fun test10() {
    val x = try {
        ""
    } finally {
        <!UNUSED_EXPRESSION!>42<!>
    }
    x.length
}