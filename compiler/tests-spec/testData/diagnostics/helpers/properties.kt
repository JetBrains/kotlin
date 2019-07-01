val nullableNumberProperty: Number? = null

val stringProperty: String = ""
val nullableStringProperty: String? = null

val intProperty: Int = ""
val nullableIntProperty: Int? = null

val implicitNullableNothingProperty = null
val nullableNothingProperty: Nothing? = null

val anonymousTypeProperty = object {}

val nullableAnonymousTypeProperty = if (true) object {} else null

val nullableOut: Out<Int>? = null

val <T> T.propT get() = 10

val <T : Any> T.propDefNotNullT get() = 10

val <T> T?.propNullableT get(): Int? = 10

val <T> T.propTT get() = 10 as T

val <T> T?.propNullableTT get() = 10 as T?

val Any.propAny get() = 10

val Any?.propNullableAny get() = 10