// IGNORE_BACKEND: JVM_IR
fun <T> builder(c: suspend () -> T): T = TODO()

class Test {
    fun doWork() {
        builder {
            execute {
                getData { getSomeString() }
            }
        }
    }

    private inline fun execute(crossinline action: suspend () -> Unit) {
        builder { action() }
    }

    private suspend fun <T> getData(dataProvider: suspend () -> T): T = builder { dataProvider() }

    private suspend fun getSomeString(): String {
        return "OK"
    }
}

// 4 TABLESWITCH