// IGNORE_BACKEND_FIR: JVM_IR
//  java.lang.UnsupportedOperationException: This function has a reified type parameter and thus can only be inlined at compilation time,
//  not called directly.
//    at kotlin.jvm.internal.Intrinsics.throwUndefinedForReified(Intrinsics.java:207)
//    at kotlin.jvm.internal.Intrinsics.throwUndefinedForReified(Intrinsics.java:201)
//    at kotlin.jvm.internal.Intrinsics.reifiedOperationMarker(Intrinsics.java:211)
//    at AdaptedArrayOfKt$box$t$1.invoke(adaptedArrayOf.kt:5)
//    at AdaptedArrayOfKt$box$t$1.invoke(adaptedArrayOf.kt:5)
//    at AdaptedArrayOfKt.test(adaptedArrayOf.kt:2)
//    at AdaptedArrayOfKt.box(adaptedArrayOf.kt:5)

fun test(f: (Int, Int) -> Array<Int>) =
    f('O'.toInt(), 'K'.toInt())

fun box(): String {
    val t = test(::arrayOf)
    return "${t[0].toChar()}${t[1].toChar()}"
}
