// IGNORE_BACKEND_FIR: JVM_IR
enum class Variants {
    O, K;
    companion object {
        val valueStr = values()[0].name + Variants.values()[1].name
    }
}

fun box() = Variants.valueStr
