fun <T> consumerOf(value: T): (item: T) -> Unit = {}

class Target {
    // The inferred type is Function1<String, Unit>. Its type argument has the parameter name `item` and the enhanced
    // nullability of the Java type, so it carries the type annotations ParameterName and EnhancedNullability.
    val consumer = consumerOf(JavaApi.name())
}
