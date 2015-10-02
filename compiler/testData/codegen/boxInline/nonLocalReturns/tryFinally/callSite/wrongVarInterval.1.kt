import test.*

fun test1(): String {
    try {
        doCall {
            try {
                doCall {
                    val a = 1
                    if (1 == 1) {
                        return "a"
                    }
                    else if (2 == 2) {
                        return "b"
                    }
                }

                return "d"
            }
            finally {
                "1"
            }
        }

    }
    finally {
        "2"
    }
    return "f"
}

fun box(): String {
    test1()

    return "OK"
}
