const val a = 1

const val increment1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!><!VARIABLE_EXPECTED!>1<!>++<!>
const val increment2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>++<!VARIABLE_EXPECTED!>1<!><!>
const val increment3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!><!VAL_REASSIGNMENT!>a<!>++<!>
const val increment4 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>++a<!>
const val increment5 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1.inc()<!>
const val increment6 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>a.inc()<!>

const val decrement1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!><!VARIABLE_EXPECTED!>1<!>++<!>
const val decrement2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>++<!VARIABLE_EXPECTED!>1<!><!>
const val decrement3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>a++<!>
const val decrement4 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>++a<!>
const val decrement5 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1.dec()<!>
const val decrement6 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>a.dec()<!>
