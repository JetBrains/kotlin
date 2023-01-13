// KT-55828
// IGNORE_BACKEND_K2: NATIVE
// SKIP_MANGLE_VERIFICATION
enum class Foo(
        val x: String,
        val callback: () -> String
) {
    FOO("OK", { FOO.x })
}

fun box() = Foo.FOO.callback()