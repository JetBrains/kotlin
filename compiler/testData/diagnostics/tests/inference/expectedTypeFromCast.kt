// !WITH_NEW_INFERENCE
// !LANGUAGE: +ExpectedTypeFromCast

fun <T> foo(): T = TODO()

fun <V> id(value: V) = value

val asString = foo() as String

val viaId = <!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>id<!>(<!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>()) as String

val insideId = id(foo() as String)

val asList = foo() as List<String>

val asStarList = foo() as List<*>

val safeAs = foo() as? String

val fromIs = <!NI;USELESS_IS_CHECK!><!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>() is String<!>
val fromNoIs = <!NI;USELESS_IS_CHECK!><!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>() !is String<!>
