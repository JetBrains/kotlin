// FIR_IDENTICAL

const val a = 1
const val b = 2

<!TYPE_CANT_BE_USED_FOR_CONST_VAL!>const<!> val rangeTo1 = 1.rangeTo(2)
<!TYPE_CANT_BE_USED_FOR_CONST_VAL!>const<!> val rangeTo2 = 1..2
<!TYPE_CANT_BE_USED_FOR_CONST_VAL!>const<!> val rangeTo3 = 1.rangeTo(b)
<!TYPE_CANT_BE_USED_FOR_CONST_VAL!>const<!> val rangeTo4 = a.rangeTo(b)
<!TYPE_CANT_BE_USED_FOR_CONST_VAL!>const<!> val rangeTo5 = 1..b
<!TYPE_CANT_BE_USED_FOR_CONST_VAL!>const<!> val rangeTo6 = a..b

<!TYPE_CANT_BE_USED_FOR_CONST_VAL!>const<!> val rangeUntil1 = 1.rangeUntil(2)
<!TYPE_CANT_BE_USED_FOR_CONST_VAL!>const<!> val rangeUntil2 = 1..<2
<!TYPE_CANT_BE_USED_FOR_CONST_VAL!>const<!> val rangeUntil3 = 1.rangeUntil(b)
<!TYPE_CANT_BE_USED_FOR_CONST_VAL!>const<!> val rangeUntil4 = a.rangeUntil(b)
<!TYPE_CANT_BE_USED_FOR_CONST_VAL!>const<!> val rangeUntil5 = 1..<b
<!TYPE_CANT_BE_USED_FOR_CONST_VAL!>const<!> val rangeUntil6 = a..<b
