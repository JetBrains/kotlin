// For KT-3491

fun <T: Any?> test1(t: Any?): Any {
    return <!UNCHECKED_CAST!>t as T<!> ?: ""
}

fun <T: Any> test2(t: Any?): Any {
    return <!UNCHECKED_CAST!>t as T<!> <!USELESS_ELVIS!>?: ""<!>
}

fun <T: Any?> test3(t: Any?): Any {
    if (t != null) {
      return t <!USELESS_ELVIS!>?: ""<!>
    }

    return 1
}