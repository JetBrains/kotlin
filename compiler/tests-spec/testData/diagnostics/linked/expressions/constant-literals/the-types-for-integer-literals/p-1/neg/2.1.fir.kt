// SKIP_TXT

// TESTCASE NUMBER: 1
fun case_1() {
    0l checkType { check<Long>() }
    10000000000000l checkType { check<Long>() }
    0X000Af10cDl checkType { check<Long>() }
    0x0_0l checkType { check<Long>() }
    0b100_000_111_111l checkType { check<Long>() }
    0b0l checkType { check<Long>() }
}
