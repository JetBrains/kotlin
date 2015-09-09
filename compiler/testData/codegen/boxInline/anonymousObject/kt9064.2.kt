package test

class Test(val _member: String) {
    val _parameter: Z =  test {
        object : Z {
            override val property = _member
        }
    }
}

interface Z {
    val property: String
}

inline fun test(s: () -> Z): Z {
    return s()
}