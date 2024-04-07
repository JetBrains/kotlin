// TARGET_BACKEND: JVM
// ISSUE: KT-65341

val nodeTransformer =
    single {
        fun getConstructorParameterValue(kParameter: String) {
            if (true) {
                throw java.lang.IllegalStateException("${kParameter!!}")
            }
        }
    }

fun <Output> single(
    singleConstructor: (NodeTransformer1<Output>) -> Output?
) {
}

class NodeTransformer1<Output>

fun box(): String {
    // Just making sure that for the original case the whole compiler pipeline is successfully completed
    return "OK"
}
