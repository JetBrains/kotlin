// No initializers for this class because the fields/properties are initialized to defaults.
object RedundantInitializersToDefault {
    // Constants

    const val constInt: Int = 0
    const val constByte: Byte = 0
    const val constLong: Long = 0L
    const val constShort: Short = 0
    const val constDouble: Double = 0.0
    const val constFloat: Float = 0.0f
    const val constBoolean: Boolean = false
    const val constChar: Char = '\u0000'

    // Properties

    var myIntPropFromConst: Int = constInt
    var myBytePropFromConst: Byte = constByte
    var myLongPropFromConst: Long = constLong
    var myShortPropFromConst: Short = constShort
    var myDoublePropFromConst: Double = constDouble
    var myFloatPropFromConst: Float = constFloat
    var myBooleanPropFromConst: Boolean = constBoolean
    var myCharPropFromConst: Char = constChar

    var myIntProp: Int = 0
    var myByteProp: Byte = 0
    var myLongProp: Long = 0L
    var myShortProp: Short = 0
    var myDoubleProp: Double = 0.0
    var myFloatProp: Float = 0.0f
    var myBooleanProp: Boolean = false
    var myCharProp: Char = '\u0000'

    var myStringProp: String? = null
    var myAnyProp: Any? = null
    var myObjectProp: java.lang.Object? = null
    var myIntegerProp: java.lang.Integer? = null

    // Fields

    @JvmField
    var myIntFieldFromConst: Int = constInt
    @JvmField
    var myByteFieldFromConst: Byte = constByte
    @JvmField
    var myLongFieldFromConst: Long = constLong
    @JvmField
    var myShortFieldFromConst: Short = constShort
    @JvmField
    var myDoubleFieldFromConst: Double = constDouble
    @JvmField
    var myFloatFieldFromConst: Float = constFloat
    @JvmField
    var myBooleanFieldFromConst: Boolean = constBoolean
    @JvmField
    var myCharFieldFromConst: Char = constChar

    @JvmField
    var myIntField: Int = 0
    @JvmField
    var myByteField: Byte = 0
    @JvmField
    var myLongField: Long = 0L
    @JvmField
    var myShortField: Short = 0
    @JvmField
    var myDoubleField: Double = 0.0
    @JvmField
    var myFloatField: Float = 0.0f
    @JvmField
    var myBooleanField: Boolean = false
    @JvmField
    var myCharField: Char = '\u0000'

    @JvmField
    var myStringField: String? = null
    @JvmField
    var myAnyField: Any? = null
    @JvmField
    var myObjectField: java.lang.Object? = null
    @JvmField
    var myIntegerField: java.lang.Integer? = null
}

object NonRedundantInitializers {
    // NOT redundant because the JVM's default values for floating-point types are positive 0.0.
    // See: https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-2.html#jvms-2.3
    var myDouble: Double = -0.0
    var myFloat: Float = -0.0f
}

// There are 20 PUTSTATUCs in RedundantInitializersToDefault are in the setters for the 20 properties that don't have @JvmField.
// There are 2 PUTSTATICs in NonRedundantInitializers are also in setters, while the other 2 are in the constructor during initialization.
// There is 1 additional PUTSTATIC for both classes for the object instance.

// 21 PUTSTATIC RedundantInitializersToDefault
// 5 PUTSTATIC NonRedundantInitializers
// 0 PUTFIELD
