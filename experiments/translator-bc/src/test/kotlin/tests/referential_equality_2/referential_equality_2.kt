class referential_equality_2_A

fun referential_equality_2_build(): referential_equality_2_A {
    return referential_equality_2_A()
}

fun referential_equality_2_EQ_slave(second_msg: referential_equality_2_A): Int {
    if (second_msg == referential_equality_2_build()) {
        return 0
    }
    return 1
}

fun referential_equality_2_NEQ_slave(second_msg: referential_equality_2_A): Int {
    if (second_msg != referential_equality_2_build()) {
        return 1
    }
    return 0
}


fun referential_equality_2_EQ_master(): Int {
    return referential_equality_2_EQ_slave(referential_equality_2_build())
}

fun referential_equality_2_NEQ_master(): Int {
    return referential_equality_2_NEQ_slave(referential_equality_2_build())
}
