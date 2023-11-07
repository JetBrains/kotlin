// ISSUE: KT-53422

class Buildee<T>

fun <T> execute(
    lambda1: Buildee<T>.(T) -> Unit,
    lambda2: Buildee<T>.(T) -> T
) {}

class Placeholder
class TargetType {
    val property: Placeholder = Placeholder()
}
fun consume(arg: TargetType) {}

@Suppress("BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION")
fun box(): String {
    execute(
        {
            consume(it)
        },
        {
            // K1&K2: UNRESOLVED_REFERENCE
            it.property
            TargetType()
        }
    )
    return "OK"
}
