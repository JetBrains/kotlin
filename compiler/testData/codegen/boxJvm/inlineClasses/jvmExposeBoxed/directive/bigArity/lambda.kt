// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// JVM_EXPOSE_BOXED

@JvmInline
value class Id(val value: Long)

class Test {
    fun <T> query(mapper: (
        String, Id, Id, Id, Id, Id, Id, Id, Id, Id, Id, Id,
        Id, Id, Id, Id, Id, Id, Id, Id, Id, Id, Id
    ) -> T): T = "OK" as T

    fun run() = query { a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23 ->
        a1
    }
}

fun box(): String {
    return Test().run()
}
