// "Change to '10000000000000000000L'" "false"
// ACTION: Convert to lazy property
// ACTION: Add 'const' modifier
// ACTION: Change type of 'a' to 'Double'
// ACTION: Convert expression to 'Long'
// ACTION: Convert property initializer to getter
// ACTION: Add underscores
// ACTION: Round using roundToLong()
// ERROR: The floating-point literal does not conform to the expected type Long

val a : Long = 10000000000000000000.0<caret>