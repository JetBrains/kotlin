class referential_equality_1

fun referential_equality_1_EQEQ_true(): Int {
    val instance = referential_equality_1()
    var instance2 = referential_equality_1()
    instance2 = instance
    //problem with boolean in auto generated functions in clang
    return if (instance == instance2) 1 else 0
}

fun referential_equality_1_EQEQ_false(): Int {
    val instance = referential_equality_1()
    val instance2 = referential_equality_1()
    return if (instance == instance2) 1 else 0
}

fun referential_equality_1_EQEQEQ_true(): Int {
    val instance = referential_equality_1()
    var instance2 = referential_equality_1()
    instance2 = instance
    return if (instance === instance2) 1 else 0
}

fun referential_equality_1_EQEQEQ_false(): Int {
    val instance = referential_equality_1()
    val instance2 = referential_equality_1()
    return if (instance === instance2) 1 else 0
}

fun referential_equality_1_EXCLEQEQ_false(): Int {
    val instance = referential_equality_1()
    var instance2 = referential_equality_1()
    instance2 = instance
    return if (instance != instance2) 1 else 0
}

fun referential_equality_1_EXCLEQEQ_true(): Int {
    val instance = referential_equality_1()
    val instance2 = referential_equality_1()
    return if (instance != instance2) 1 else 0
}

fun referential_equality_1_EXCLEQEQEQ_false(): Int {
    val instance = referential_equality_1()
    var instance2 = referential_equality_1()
    instance2 = instance
    return if (instance !== instance2) 1 else 0
}

fun referential_equality_1_EXCLEQEQEQ_true(): Int {
    val instance = referential_equality_1()
    val instance2 = referential_equality_1()
    return if (instance !== instance2) 1 else 0
}