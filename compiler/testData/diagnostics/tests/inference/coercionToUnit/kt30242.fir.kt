// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER
// ISSUE: KT-30242

class A

fun println(s: String = "") {}

fun foo(f: () -> Any) {}

fun test1(b: Boolean) {
    foo {
        if (b) {
            println("meh")
        }
    }
}

fun test2(b: Boolean) {
    foo {
        when {
            b -> println("meh")
        }
    }
}

fun test3(b: Boolean) {
    foo {
        if (b) {
            return@foo A()
        }
    }
}

fun test4(b: Boolean) {
    foo {
        if (b) {
            return@foo println("meh")
        }

        if (b) {
            println()
        }
    }
}

fun bar(block: () -> String) {}

fun test_5(b: Boolean) {
    bar {
        <!ARGUMENT_TYPE_MISMATCH!>if (b) {
            println("meh")
        }<!>
    }
}

fun test_6(b: Boolean) {
    foo {
        if (b) {
            return@foo Unit
        }
        if (b) {}
    }
}
