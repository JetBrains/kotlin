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

// JVM_TEMPLATES
// suspend lambdas: 4
// suspend lambdas $$forInline: 1
// 5 TABLESWITCH

// JVM_IR_TEMPLATES
// tail-call suspend lambdas: 2
// 3 TABLESWITCH