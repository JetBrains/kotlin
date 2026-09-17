// FullValueClassesKt
// LANGUAGE: +FullValueClasses
// WITH_STDLIB

fun getSingleField(): SingleField = SingleField(42)
fun getNullableSingleField(): SingleField? = null
fun getMultiField(): MultiField = MultiField(1, 2)
fun getNullableMultiField(): MultiField? = null

value class SingleField(val data: Int)
value class MultiField(val first: Int, val second: Int)
