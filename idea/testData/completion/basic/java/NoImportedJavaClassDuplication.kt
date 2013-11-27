import java.io.InputStreamReader

val x = InputStreamReader<caret>

// INVOCATION_COUNT: 1
// EXIST: { lookupString:"InputStreamReader", tailText:" (java.io)" }
// NUMBER: 1