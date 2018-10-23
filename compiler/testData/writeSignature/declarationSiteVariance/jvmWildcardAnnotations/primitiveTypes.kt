// WITH_RUNTIME
class Out<out T>
class In<in E>

@JvmSuppressWildcards(false)
fun foo(x: Boolean, y: Out<Int>): Int = 1
// method: PrimitiveTypesKt::foo
// generic signature: (ZLOut<+Ljava/lang/Integer;>;)I

@JvmSuppressWildcards(true)
fun bar(x: Boolean, y: In<Long>, z: @JvmSuppressWildcards(false) Long): Int = 1
// method: PrimitiveTypesKt::bar
// generic signature: (ZLIn<Ljava/lang/Long;>;J)I
