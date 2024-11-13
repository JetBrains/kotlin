// ISSUE: KT-73087
// IGNORE_LIGHT_ANALYSIS
// IGNORE_BACKEND: ANY
// ^^^ KT-73087: No override for FUN DEFAULT_PROPERTY_ACCESSOR name:<get-privateLam> visibility:private modality:FINAL <> ($this:<root>.Clazz) returnType:kotlin.Function0<kotlin.String> in CLASS CLASS name:<no name provided> modality:FINAL visibility:local superTypes:[<root>.Clazz]

abstract class Clazz(private val privateLam: () -> String) {
    abstract fun foo(): String
}

fun box(): String {
    val clazz = object : Clazz({ "OK" }) {
        @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
        override fun foo(): String {
            return privateLam.invoke()
        }
    }
    return clazz.foo()
}
