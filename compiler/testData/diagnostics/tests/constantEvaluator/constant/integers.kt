// WITH_STDLIB
package test

// val prop1: 513105426295.toLong()
<!DEBUG_INFO_CONSTANT_VALUE("513105426295.toLong()")!>val prop1: Int = <!CONSTANT_EXPECTED_TYPE_MISMATCH!>0x7777777777<!><!>

// val prop2: 513105426295.toLong()
<!DEBUG_INFO_CONSTANT_VALUE("513105426295.toLong()")!>val prop2: Long = 0x7777777777<!>

// val prop3: 513105426295.toLong()
<!DEBUG_INFO_CONSTANT_VALUE("513105426295.toLong()")!>val prop3 = 0x7777777777<!>

// val prop4: 10
<!DEBUG_INFO_CONSTANT_VALUE("10")!>const val prop4 = '\n'.code<!>
