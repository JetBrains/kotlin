fun simple() = fun (): Boolean { return true }

fun simpleNamed() = fun name(): Boolean { return true }
fun simpleNamed2() = fun name(): Boolean { return@name true }

fun withLabel() = @l fun (): Boolean { return@l true }
fun withLabelNamed() = @l fun name(): Boolean { return@l true }

fun box(): String {
    if (!simple()()) return "Test simple failed"
    if (!simpleNamed()()) return "Test simpleNamed failed"
    if (!simpleNamed2()()) return "Test simpleNamed2 failed"
    if (!withLabel()()) return "Test withLabel failed"
    if (!withLabelNamed()()) return "Test withLabelNamed failed"

    return "OK"
}