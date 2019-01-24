// "Change to '10000000000000000000L'" "false"
// ACTION: Add 'const' modifier
// ACTION: Change type of 'a' to 'Double'
// ACTION: Convert expression to 'Long'
// ACTION: Convert property initializer to getter
// ACTION: Add underscores
// ERROR: The floating-point literal does not conform to the expected type Long

val a : Long = 10000000000000000000.0<caret>