// !DIAGNOSTICS: -UNUSED_PARAMETER -PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED
<!MULTIPLE_VARARG_PARAMETERS!>fun test(vararg x1: Int, vararg x2: Int)<!> {
    <!MULTIPLE_VARARG_PARAMETERS!>fun test2(vararg x1: Int, vararg x2: Int)<!> {
        class LocalClass<!MULTIPLE_VARARG_PARAMETERS!>(vararg x1: Int, vararg x2: Int)<!> {
            <!MULTIPLE_VARARG_PARAMETERS!>constructor(vararg x1: Int, vararg x2: Int, xx: Int)<!> {}
        }
        <!MULTIPLE_VARARG_PARAMETERS!>fun test3(vararg x1: Int, vararg x2: Int)<!> {}
    }
}

<!MULTIPLE_VARARG_PARAMETERS!>fun Any.test(vararg x1: Int, vararg x2: Int, vararg x3: Int)<!> {}

interface I {
    <!MULTIPLE_VARARG_PARAMETERS!>fun test(vararg x1: Int, vararg x2: Int)<!>
}

abstract class C<!MULTIPLE_VARARG_PARAMETERS!>(vararg x1: Int, vararg x2: Int, b: Boolean)<!> {
    <!MULTIPLE_VARARG_PARAMETERS!>fun test(vararg x1: Int, vararg x2: Int)<!> {}

    <!MULTIPLE_VARARG_PARAMETERS!>abstract fun test2(vararg x1: Int, vararg x2: Int)<!>

    class CC<!MULTIPLE_VARARG_PARAMETERS!>(vararg x1: Int, vararg x2: Int, b: Boolean)<!> {
        <!MULTIPLE_VARARG_PARAMETERS!>constructor(vararg x1: Int, vararg x2: Int)<!> {}
        <!MULTIPLE_VARARG_PARAMETERS!>fun test(vararg x1: Int, vararg x2: Int)<!> {}
    }
}

