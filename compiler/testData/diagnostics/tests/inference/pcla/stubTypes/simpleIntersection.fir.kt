fun <R1> build(block: TestInterface<R1>.() -> Unit): R1 = TODO()

interface TestInterface<R> {
    fun emit(r: R)
    fun get(): R
}

fun <X> select(x: X, y: X): X = x

fun test() {
    val ret0 = build l1@{
        emit("1")

        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit")!>build l2@{
            emit(1)
            select(this@l1.get(), get())
        }<!>
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Comparable<*> & java.io.Serializable")!>ret0<!>

    val ret1 = build l1@{
        emit("1")

        build l2@{
            emit(1)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>select(this@l1.get(), get())<!>
        }
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Comparable<*> & java.io.Serializable")!>ret1<!>

    val ret2 = build l1@{
        emit("1")

        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>build l2@{
            emit(1)
            select(this@l1.get(), get())
            ""
        }<!>
        ""
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>ret2<!>

    val ret3 = build l1@{
        emit("1")

        build l2@{
            emit(1)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Comparable<*> & java.io.Serializable")!>select(this@l1.get(), get())<!>
            ""
        }
        ""
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>ret3<!>
}