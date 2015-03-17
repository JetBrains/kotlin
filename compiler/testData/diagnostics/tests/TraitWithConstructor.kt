class C(val a: String) {}

trait T1<!CONSTRUCTOR_IN_TRAIT!>(val x: String)<!> {}

trait T2<!CONSTRUCTOR_IN_TRAIT!>()<!> {}

trait T3<!CONSTRUCTOR_IN_TRAIT!>(<!UNUSED_PARAMETER!>a<!>: Int)<!> {}

trait T4 {
    <!CONSTRUCTOR_IN_TRAIT!>constructor(a: <!DEBUG_INFO_MISSING_UNRESOLVED!>Int<!>) {
        val b: <!DEBUG_INFO_MISSING_UNRESOLVED!>Int<!> = 1
    }<!>
}