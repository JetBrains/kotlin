//constructor annotation/Target(vararg annotation/AnnotationTarget)
//│     enum class annotation/AnnotationTarget: Enum<annotation/AnnotationTarget>
//│     │                enum entry annotation/AnnotationTarget.EXPRESSION
//│     │                │           enum class annotation/AnnotationTarget: Enum<annotation/AnnotationTarget>
//│     │                │           │                enum entry annotation/AnnotationTarget.LOCAL_VARIABLE
//│     │                │           │                │
@Target(AnnotationTarget.EXPRESSION, AnnotationTarget.LOCAL_VARIABLE)
//constructor annotation/Retention(annotation/AnnotationRetention = ...)
//│        enum class annotation/AnnotationRetention: Enum<annotation/AnnotationRetention>
//│        │                   enum entry annotation/AnnotationRetention.SOURCE
//│        │                   │
@Retention(AnnotationRetention.SOURCE)
annotation class Ann

fun foo(arg: Int): Int {
//       constructor Ann()
//       │   foo.arg: Int
//       │   │   EQ operator call
//  Unit │   │   │  Int
//  │    │   │   │  │
    if (@Ann arg == 0) {
//       constructor Ann()
//       │          Int
//       │          │
        @Ann return 1
    }
//   constructor Ann()
//   │   Unit
//   │   │   foo.arg: Int
//   │   │   │   EQ operator call
//   │   │   │   │  Int
//   │   │   │   │  │
    @Ann if (arg == 1) {
//               constructor Ann()
//               │   Int
//               │   │
        return (@Ann 1)
    }
//         Int
//         │
    return 42
}

data class Two(val x: Int, val y: Int)

fun bar(two: Two) {
//        constructor Ann()
//        │       constructor Ann()
//        │   Int │   Int  bar.two: Two
//        │   │   │   │    │
    val (@Ann x, @Ann y) = two
}
