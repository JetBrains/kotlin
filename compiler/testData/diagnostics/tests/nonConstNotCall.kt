// ISSUE: KT-55196

const val bb: Boolean = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>!false<!> // Red in K1, green in K2
