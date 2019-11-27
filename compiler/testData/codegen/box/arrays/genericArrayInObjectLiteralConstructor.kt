// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
abstract class Table<T>(
        val content: Array<Array<T>>
)

fun box(): String {
    val x = object : Table<String>(
            Array(1, {
                x-> Array(1, {y -> "OK"})
            })
    ) {}

    return x.content[0][0]
}
