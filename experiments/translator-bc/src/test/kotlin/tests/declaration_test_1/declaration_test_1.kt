class declaration_test_1_cls()

fun declaration_test_1_variable(): Int {
    val a: Int
    a = 85
    return a
}

fun declaration_test_1_class_slave(): declaration_test_1_cls {
    val a: declaration_test_1_cls
    a = declaration_test_1_cls()
    return a
}


fun declaration_test_1_class(): Int {
    return if (declaration_test_1_class_slave() == null) 10 else 96799
}