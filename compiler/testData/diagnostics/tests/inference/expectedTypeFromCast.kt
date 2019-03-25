// !WITH_NEW_INFERENCE
// !LANGUAGE: +ExpectedTypeFromCast

fun <T> foo(): T = TODO()

fun <V> id(value: V) = value

val asString = foo() as String

val viaId = <!NI;IMPLICIT_NOTHING_AS_TYPE_PARAMETER, OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>id<!>(<!NI;IMPLICIT_NOTHING_AS_TYPE_PARAMETER, NI;IMPLICIT_NOTHING_AS_TYPE_PARAMETER, OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>()) as String

val insideId = id(foo() as String)

val asList = foo() as List<String>

val asStarList = foo() as List<*>

val safeAs = foo() as? String

val fromIs = <!NI;USELESS_IS_CHECK!><!NI;IMPLICIT_NOTHING_AS_TYPE_PARAMETER, OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>() is String<!>
val fromNoIs = <!NI;USELESS_IS_CHECK!><!NI;IMPLICIT_NOTHING_AS_TYPE_PARAMETER, OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>() !is String<!>
