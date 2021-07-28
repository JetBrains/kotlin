package test

// val prop1: 1.toLong()
<!DEBUG_INFO_CONSTANT_VALUE("1.toLong()")!>val prop1 = 1L<!>

// val prop2: 1.toLong()
<!DEBUG_INFO_CONSTANT_VALUE("1.toLong()")!>val prop2 = 0x1L<!>

// val prop3: 1.toLong()
<!DEBUG_INFO_CONSTANT_VALUE("1.toLong()")!>val prop3 = 0X1L<!>

// val prop4: 1.toLong()
<!DEBUG_INFO_CONSTANT_VALUE("1.toLong()")!>val prop4 = 0b1L<!>

// val prop5: 1.toLong()
<!DEBUG_INFO_CONSTANT_VALUE("1.toLong()")!>val prop5 = 0B1L<!>

// val prop6: 1.toLong()
<!DEBUG_INFO_CONSTANT_VALUE("1.toLong()")!>val prop6 = 1<!WRONG_LONG_SUFFIX!>l<!><!>

// val prop7: 1.toLong()
<!DEBUG_INFO_CONSTANT_VALUE("1.toLong()")!>val prop7 = 0x1<!WRONG_LONG_SUFFIX!>l<!><!>

// val prop8: 1.toLong()
<!DEBUG_INFO_CONSTANT_VALUE("1.toLong()")!>val prop8 = 0X1<!WRONG_LONG_SUFFIX!>l<!><!>

// val prop9: 1.toLong()
<!DEBUG_INFO_CONSTANT_VALUE("1.toLong()")!>val prop9 = 0b1<!WRONG_LONG_SUFFIX!>l<!><!>

// val prop10: 1.toLong()
<!DEBUG_INFO_CONSTANT_VALUE("1.toLong()")!>val prop10 = 0B1<!WRONG_LONG_SUFFIX!>l<!><!>
