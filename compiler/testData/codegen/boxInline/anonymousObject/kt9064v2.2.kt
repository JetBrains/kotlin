package test

class Test(val _member: String) {
    val _parameter: Z<Z<String>> =  test {
        object : Z<Z<String>> {
            override val property = test {
                object : Z<String> {
                    override val property = _member
                }
            }
        }
    }
}

interface Z<T> {
    val property: T
}

inline fun <T> test(s: () -> Z<T>): Z<T> {
    return s()
}