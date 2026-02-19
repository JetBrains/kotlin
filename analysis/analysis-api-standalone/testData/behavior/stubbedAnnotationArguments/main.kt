annotation class MyAnnotation(val tag: ByteArray)

class Main(
    @MyAnnotation(
        tag = [1.toByte(), 2.toByte(), 3.toByte()]
    )
    val p0: String = ""
)