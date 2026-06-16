import kotlin.reflect.*

fun main() {
    val unit = Array<Any>::set.returnType.classifier
    if (unit != Unit::class)
        throw AssertionError("Unexpected classifier: $unit")
}
