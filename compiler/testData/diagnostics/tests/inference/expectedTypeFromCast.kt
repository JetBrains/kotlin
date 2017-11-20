// !WITH_NEW_INFERENCE
// !LANGUAGE: +ExpectedTypeFromCast

fun <T> foo(): T = TODO()

fun <V> id(value: V) = value

val asString = foo() as String

val viaId = <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>id<!>(<!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>()) as String

val insideId = id(foo() as String)

val asList = foo() as List<String>

val asStarList = foo() as List<*>

val safeAs = foo() as? String

val fromIs = <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>() is String
val fromNoIs = <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>() !is String
