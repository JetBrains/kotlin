// IGNORE_BACKEND_FIR: JVM_IR
enum class Foo(
        val x: String,
        val callback: () -> String
) {
    FOO("OK", { FOO.x })
}

fun box() = Foo.FOO.callback()