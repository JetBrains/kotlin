val nonConstBool = true
const val constBool = false

const val andExpr1 = true && false
const val andExpr2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>nonConstBool && false<!>
const val andExpr3 = true && constBool
const val andExpr4 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>nonConstBool && constBool<!>

const val orExpr1 = true || false
const val orExpr2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>nonConstBool || false<!>
const val orExpr3 = true || constBool
const val orExpr4 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>nonConstBool || constBool<!>
