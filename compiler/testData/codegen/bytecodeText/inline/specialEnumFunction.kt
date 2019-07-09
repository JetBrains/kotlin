// IGNORE_BACKEND: JVM_IR
inline fun <reified T : Enum<T>> myValues(): String {
    val values = enumValues<T>()
    return "OK"
}

inline fun <reified T : Enum<T>> value(): String {
    val values = enumValueOf<T>("123")
    return "OK"
}
enum class Z
fun main() {
    myValues<Z>()
    value<Z>()
}

//2 reifiedOperationMarker
//1 INVOKESTATIC kotlin/jvm/internal/Intrinsics\.reifiedOperationMarker \(ILjava/lang/String;\)V\s*ICONST_0\s*ANEWARRAY java/lang/Enum
//1 INVOKESTATIC Z\.values \(\)\[LZ;

//4 valueOf
//1 INVOKESTATIC kotlin/jvm/internal/Intrinsics\.reifiedOperationMarker \(ILjava/lang/String;\)V\s*ACONST_NULL\s*ALOAD 2\s*INVOKESTATIC java/lang/Enum\.valueOf \(Ljava/lang/Class;Ljava/lang/String;\)Ljava/lang/Enum;
//1 INVOKESTATIC Z\.valueOf \(Ljava/lang/String;\)LZ;
//1 public static valueOf
//2 INVOKESTATIC java/lang/Enum.valueOf \(Ljava/lang/Class;Ljava/lang/String;\)Ljava/lang/Enum;
