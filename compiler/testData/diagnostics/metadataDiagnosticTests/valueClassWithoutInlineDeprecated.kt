// METADATA_TARGET_PLATFORMS: JS, WasmJs
// LANGUAGE: +AllowExpectValueClassesWithNoPrimaryConstructor

import kotlin.jvm.JvmInline

<!INLINE_VALUE_CLASS_WITHOUT_EXPLICIT_INLINE_WARNING!>value<!> class Val1(val x: Int)

expect <!INLINE_VALUE_CLASS_WITHOUT_EXPLICIT_INLINE_WARNING!>value<!> class Val2

@JvmInline
value class Val3(val x: Int)

inline value class Val4(val x: Int)

expect inline value class Val5

@JvmInline
expect value class Val6(val x: Int)

@JvmInline
expect value class Val7
