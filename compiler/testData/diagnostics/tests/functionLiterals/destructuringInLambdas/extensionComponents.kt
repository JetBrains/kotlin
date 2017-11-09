// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_PARAMETER
class A

operator fun A.component1() = 1
operator fun A.component2() = ""

class B

class D {
    operator fun A.component1() = 1.0
    operator fun A.component2() = ' '

    operator fun B.component1() = 1.0
    operator fun B.component2() = ' '
}

fun foo(block: (A) -> Unit) { }
fun foobaz(block: D.(B) -> Unit) { }
fun foobar(block: D.(A) -> Unit) { }

fun bar() {
    foo { (a, b) ->
        a checkType { _<Int>() }
        b checkType { _<String>() }
    }

    foo { (a: Int, b: String) ->
        a checkType { _<Int>() }
        b checkType { _<String>() }
    }

    // From KEEP: Component-functions are resolved in the same scope as the first statement of the lambda
    foobaz { (a, b) ->
        a checkType { _<Double>() }
        b checkType { _<Char>() }
    }

    // From KEEP: Component-functions are resolved in the same scope as the first statement of the lambda
    // So `component1`/`component2` for lambda parameters were resolved to member extensions
    foobar { (a, b) ->
        a checkType { _<Double>() }
        b checkType { _<Char>() }
    }

    // the following code fails with exception, see KT-13873
//    foobarbaz {
//        component1: B.() -> Int,
//        component2: B.() -> String,
//        (a, b): B ->
//
//    }
}
