// FIR_IGNORE
//constructor annotation/Target(vararg annotation/AnnotationTarget)
//│     enum class annotation/AnnotationTarget: Enum<annotation/AnnotationTarget>
//│     │                enum entry annotation/AnnotationTarget.EXPRESSION
//│     │                │
@Target(AnnotationTarget.EXPRESSION)
//constructor annotation/Retention(annotation/AnnotationRetention = ...)
//│        enum class annotation/AnnotationRetention: Enum<annotation/AnnotationRetention>
//│        │                   enum entry annotation/AnnotationRetention.SOURCE
//│        │                   │
@Retention(AnnotationRetention.SOURCE)
annotation class Ann

fun foo(arg: Int): Int {
//       constructor Ann()
//       │   foo.arg: Int
//       │   │   fun (Any).equals(Any?): Boolean
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
//   │   │   │   fun (Any).equals(Any?): Boolean
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

data class Two(x: Int, y: Int)

fun bar(two: Two) {
//        constructor Ann()
//        │   [ERROR : component1() return type]
//        │   │   constructor Ann()
//        │   │   │   [ERROR : component2() return type]
//        │   │   │   │    bar.two: Two
//        │   │   │   │    │
    val (@Ann x, @Ann y) = two
}
