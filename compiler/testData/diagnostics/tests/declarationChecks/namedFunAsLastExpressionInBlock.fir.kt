// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_ANONYMOUS_PARAMETER -UNUSED_VARIABLE
// !CHECK_TYPE
fun foo(block: () -> (() -> Int)) {}

fun test() {
    val x = <!INFERENCE_ERROR, INFERENCE_ERROR, INFERENCE_ERROR!>fun named1(x: Int): Int { return 1 }<!>
    x <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><Function1<Int, Int>>() }

    foo { fun named2(): Int {return 1} }
    foo({ fun named3() = 1 })

    val x1 =
    if (1 == 1)
    // TODO: Diagnostic content could be better
    <!SYNTAX!><!>fun named4(): Int {return 1}
    <!SYNTAX!>else<!>
    fun named5() = 1

    val x2 =
    if (1 == 1) {
        fun named6(): Int {
            return 1
        }
    }
    else
    <!SYNTAX!><!>fun named7() = 1

    val x3 = when (1) {
        0 -> fun named8(): Int {return 1}
        else -> fun named9() = 1
    }

    val x31 = when (1) {
        0 -> {
            fun named10(): Int {return 1}
        }
        else -> fun named11() = 1
    }

    val x4 = {
        y: Int -> fun named12(): Int {return 1}
    }

    x4 checkType { <!UNRESOLVED_REFERENCE!>_<!><Function1<Int, Unit>>() }

    { y: Int -> fun named14(): Int {return 1} }
    val b = <!UNRESOLVED_REFERENCE!>(<!INFERENCE_ERROR!>fun named15(): Boolean { return true }<!>)()<!>

    baz(<!INFERENCE_ERROR!>fun named16(){}<!>)
}

fun bar() = <!INFERENCE_ERROR, INFERENCE_ERROR, INFERENCE_ERROR!>fun named() {}<!>

fun <T> run(block: () -> T): T = null!!
fun run2(block: () -> Unit): Unit = null!!
fun baz(obj: Any?) {}

fun success() {
    run { fun named1() = 1 }
    run2 { fun named2() = 1 }

    val x = run { fun named3() = 1 }
    x checkType { <!UNRESOLVED_REFERENCE!>_<!><Unit>() }

    val y = when (1) {
        0 -> {
            fun named4(): Int {return 1}
        }
        else -> {
            fun named5(): Int {return 1}
        }
    }
    y checkType { <!UNRESOLVED_REFERENCE!>_<!><Unit>() }
}
