const val DOUBLE_BATCH_SIZE = 2 * BATCH_SIZE

annotation class A(val value: Int)

@A(BATCH_SIZE)
fun bar() {}