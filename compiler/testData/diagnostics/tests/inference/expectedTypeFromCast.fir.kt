// !LANGUAGE: +ExpectedTypeFromCast

fun <T> foo(): T = TODO()

fun <V> id(value: V) = value

val asString = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>() as String

val viaId = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>id<!>(foo()) as String

val insideId = id(<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>() as String)

val asList = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>() as List<String>

val asStarList = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>() as List<*>

val safeAs = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>() as? String

val fromIs = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>() is String
val fromNoIs = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>() !is String
