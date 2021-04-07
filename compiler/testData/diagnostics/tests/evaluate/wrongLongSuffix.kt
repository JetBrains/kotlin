// FIR_IDENTICAL
val a1: Int = 1<!WRONG_LONG_SUFFIX!>l<!>
val a2: Int = 0x1<!WRONG_LONG_SUFFIX!>l<!>
val a3: Int = 0X1<!WRONG_LONG_SUFFIX!>l<!>
val a4: Int = 0b1<!WRONG_LONG_SUFFIX!>l<!>
val a5: Int = 0B1<!WRONG_LONG_SUFFIX!>l<!>
val a6: Long = 1<!WRONG_LONG_SUFFIX!>l<!>
val a7: Long = 0x1<!WRONG_LONG_SUFFIX!>l<!>
val a8: Long = 0X1<!WRONG_LONG_SUFFIX!>l<!>
val a9: Long = 0b1<!WRONG_LONG_SUFFIX!>l<!>
val a10: Long = 0B1<!WRONG_LONG_SUFFIX!>l<!>