// RUN_PIPELINE_TILL: FRONTEND
val nonConstBool = true
const val constBool = false

const val andExpr1 = true && false
const val andExpr2 = <!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>nonConstBool && false<!>
const val andExpr3 = true && constBool
const val andExpr4 = <!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>nonConstBool && constBool<!>

const val orExpr1 = true || false
const val orExpr2 = <!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>nonConstBool || false<!>
const val orExpr3 = true || constBool
const val orExpr4 = <!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>nonConstBool || constBool<!>
