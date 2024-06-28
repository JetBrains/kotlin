// WITH_STDLIB

open class Arguments {
    @GradleOption(
        value = DefaultValue.BOOLEAN_FALSE_DEFAULT,
        gradleInputType = GradleInputTypes.INPUT,
    )
    val useK2: Boolean by lazy { false }
}

class JvmArguments : Arguments() {
    @GradleOption(
        value = DefaultValue.BOOLEAN_FALSE_DEFAULT,
        gradleInputType = GradleInputTypes.INPUT,
    )
    val specific: Boolean by lazy { true }
}

@Retention(AnnotationRetention.RUNTIME)
annotation class GradleOption(
    val value: DefaultValue,
    val gradleInputType: GradleInputTypes
)

enum class GradleInputTypes(
    val typeAsString: String
) {
    INPUT("org.gradle.api.tasks.Input"),
    INTERNAL("org.gradle.api.tasks.Internal");

    override fun toString(): String {
        return typeAsString
    }
}

enum class DefaultValue {
    BOOLEAN_FALSE_DEFAULT,
    BOOLEAN_TRUE_DEFAULT,
}

fun box(): String {
    return "OK"
}
