// No initializers for this class because the fields/properties are initialized to defaults.
class RedundantInitializersToDefault {
    companion object {
        // Constants
        const val constInt: Int = 0
        const val constByte: Byte = 0
        const val constLong: Long = 0L
        const val constShort: Short = 0
        const val constDouble: Double = 0.0
        const val constFloat: Float = 0.0f
        const val constBoolean: Boolean = false
        const val constChar: Char = '\u0000'
    }

    // Properties

    val myIntPropFromConst: Int = constInt
    val myBytePropFromConst: Byte = constByte
    val myLongPropFromConst: Long = constLong
    val myShortPropFromConst: Short = constShort
    val myDoublePropFromConst: Double = constDouble
    val myFloatPropFromConst: Float = constFloat
    val myBooleanPropFromConst: Boolean = constBoolean
    val myCharPropFromConst: Char = constChar

    val myIntProp: Int = 0
    val myByteProp: Byte = 0
    val myLongProp: Long = 0L
    val myShortProp: Short = 0
    val myDoubleProp: Double = 0.0
    val myFloatProp: Float = 0.0f
    val myBooleanProp: Boolean = false
    val myCharProp: Char = '\u0000'

    val myStringProp: String? = null
    val myAnyProp: Any? = null
    val myObjectProp: java.lang.Object? = null
    val myIntegerProp: java.lang.Integer? = null

    // Fields

    @JvmField
    val myIntFieldFromConst: Int = constInt
    @JvmField
    val myByteFieldFromConst: Byte = constByte
    @JvmField
    val myLongFieldFromConst: Long = constLong
    @JvmField
    val myShortFieldFromConst: Short = constShort
    @JvmField
    val myDoubleFieldFromConst: Double = constDouble
    @JvmField
    val myFloatFieldFromConst: Float = constFloat
    @JvmField
    val myBooleanFieldFromConst: Boolean = constBoolean
    @JvmField
    val myCharFieldFromConst: Char = constChar

    @JvmField
    val myIntField: Int = 0
    @JvmField
    val myByteField: Byte = 0
    @JvmField
    val myLongField: Long = 0L
    @JvmField
    val myShortField: Short = 0
    @JvmField
    val myDoubleField: Double = 0.0
    @JvmField
    val myFloatField: Float = 0.0f
    @JvmField
    val myBooleanField: Boolean = false
    @JvmField
    val myCharField: Char = '\u0000'

    @JvmField
    val myStringField: String? = null
    @JvmField
    val myAnyField: Any? = null
    @JvmField
    val myObjectField: java.lang.Object? = null
    @JvmField
    val myIntegerField: java.lang.Integer? = null
}

class NonRedundantInitializers {
    // NOT redundant because the JVM's default values for floating-point types are positive 0.0.
    // See: https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-2.html#jvms-2.3
    val myDouble: Double = -0.0
    val myFloat: Float = -0.0f
}

// 0 PUTFIELD RedundantInitializersToDefault
// 2 PUTFIELD NonRedundantInitializers
