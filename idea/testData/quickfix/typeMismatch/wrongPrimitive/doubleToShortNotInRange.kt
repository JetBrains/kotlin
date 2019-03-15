// "Change to '65000'" "false"
// ACTION: Add 'const' modifier
// ACTION: Change type of 'a' to 'Double'
// ACTION: Convert expression to 'Short'
// ACTION: Convert property initializer to getter
// ACTION: Add underscores
// ERROR: The floating-point literal does not conform to the expected type Short

val a : Short = 65000.0<caret>