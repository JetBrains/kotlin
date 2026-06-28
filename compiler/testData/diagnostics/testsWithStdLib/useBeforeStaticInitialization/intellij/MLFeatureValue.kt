// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// WITH_STDLIB
interface MLFeatureValueBase {
    val value: Any
}

<!POSSIBLE_INITIALIZATION_DEADLOCK!>sealed class MLFeatureValue : MLFeatureValueBase {
    companion object {
        private val TRUE = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>BinaryValue(true)<!>
        private val FALSE = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>BinaryValue(false)<!>

        @JvmStatic
        fun binary(value: Boolean): MLFeatureValue = if (value) TRUE else FALSE

        @JvmStatic
        fun float(value: Int): MLFeatureValue = FloatValue(value.toDouble())

        @JvmStatic
        fun float(value: Double): MLFeatureValue = FloatValue(value)

        // alias for float(Int), but could be used from java sources (since java forbids to use method named like a keyword)
        @JvmStatic
        fun numerical(value: Int): MLFeatureValue = float(value)

        // alias for float(Double), but could be used from java sources (since java forbids to use method named like a keyword)
        @JvmStatic
        fun numerical(value: Double): MLFeatureValue = float(value)

        @JvmStatic
        fun <T : Enum<*>> categorical(value: T): MLFeatureValue = CategoricalValue(value.toString())

        @JvmStatic
        fun <T : Class<*>> className(value: T, useSimpleName: Boolean = true): MLFeatureValue = ClassNameValue(value, useSimpleName)

        @JvmStatic
        fun version(value: String): MLFeatureValue = VersionValue(value)
    }

    <!POSSIBLE_INITIALIZATION_DEADLOCK!>data class BinaryValue internal constructor(override val value: Boolean) : MLFeatureValue()<!>
    data class FloatValue internal constructor(override val value: Double) : MLFeatureValue()
    data class CategoricalValue internal constructor(override val value: String) : MLFeatureValue()
    data class ClassNameValue internal constructor(override val value: Class<*>, val useSimpleName: Boolean) : MLFeatureValue()
    data class VersionValue internal constructor(override val value: String) : MLFeatureValue()
}<!>

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, data, functionDeclaration, ifExpression, interfaceDeclaration,
nestedClass, objectDeclaration, override, primaryConstructor, propertyDeclaration, sealed, starProjection,
typeConstraint, typeParameter */
