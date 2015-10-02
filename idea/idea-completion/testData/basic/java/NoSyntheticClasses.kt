import kotlin.properties.*

val x: ReadOnlyPr<caret>

// INVOCATION_COUNT: 2
// EXIST: "ReadOnlyProperty"
// ABSENT: "ReadOnlyProperty$DefaultImpls"
// NOTHING_ELSE
