// "Cast expression '1' to 'UInt'" "false"
// WITH_RUNTIME
// ERROR: Conversion of signed constants to unsigned ones is prohibited
// ACTION: Convert to lazy property
// ACTION: Change parameter 'u' type of function 'takeUInt' to 'Int'
// ACTION: Convert property initializer to getter
// ACTION: Change to '1u'
// ACTION: Do not show hints for current method
// ACTION: Add 'u =' to argument
// COMPILER_ARGUMENTS: -XXLanguage:-NewInference

fun takeUInt(u: UInt) = 0

val b = takeUInt(<caret>1)