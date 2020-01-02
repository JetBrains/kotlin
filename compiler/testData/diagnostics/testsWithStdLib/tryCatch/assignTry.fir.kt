// !WITH_NEW_INFERENCE
// SKIP_TXT

class ExcA : Exception()

class ExcB : Exception()

fun test2() {
    val s: String? = try {
        ""
    }
    catch (e: ExcA) {
        null
    }
    catch (e: ExcB) {
        10
    }
    s.<!INAPPLICABLE_CANDIDATE!>length<!>
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
    s.<!INAPPLICABLE_CANDIDATE!>length<!>
}

fun test4() {
    val s: String? = try {
        ""
    }
    catch (e: ExcA) {
        null
    }
    finally {
        ""
    }
    s.<!INAPPLICABLE_CANDIDATE!>length<!>
}

fun test5() {
    val s: String? = try {
        ""
    }
    catch (e: ExcA) {
        null
    }
    finally {
        return
    }
    s.<!INAPPLICABLE_CANDIDATE!>length<!>
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
    s.<!INAPPLICABLE_CANDIDATE!>length<!>
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
    s.<!INAPPLICABLE_CANDIDATE!>length<!>
}

fun test8() {
    val s = try {
        ""
    } catch (e: ExcA) {
        null
    }
    s.<!INAPPLICABLE_CANDIDATE!>length<!>
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
        42
    }
    x.length
}