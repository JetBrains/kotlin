// SKIP_TXT

// TESTCASE NUMBER: 1
fun case_1(value_1: TypesProvider) {
    when {
        getBoolean(), value_1.getBoolean()  -> return
        value_1.getBoolean() && getBoolean(), getLong() == 1000L -> return
        Out<Int>(), getLong(), {}, Any(), throw Exception() -> return
    }

    return
}
