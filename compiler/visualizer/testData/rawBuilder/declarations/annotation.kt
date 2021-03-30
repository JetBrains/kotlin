// FIR_IGNORE
//constructor annotation/Target(vararg annotation/AnnotationTarget)
//│
@Target(
//  enum class annotation/AnnotationTarget: Enum<annotation/AnnotationTarget>
//  │                enum entry annotation/AnnotationTarget.CLASS
//  │                │
    AnnotationTarget.CLASS,
//  enum class annotation/AnnotationTarget: Enum<annotation/AnnotationTarget>
//  │                enum entry annotation/AnnotationTarget.PROPERTY
//  │                │
    AnnotationTarget.PROPERTY,
//  enum class annotation/AnnotationTarget: Enum<annotation/AnnotationTarget>
//  │                enum entry annotation/AnnotationTarget.LOCAL_VARIABLE
//  │                │
    AnnotationTarget.LOCAL_VARIABLE,
//  enum class annotation/AnnotationTarget: Enum<annotation/AnnotationTarget>
//  │                enum entry annotation/AnnotationTarget.VALUE_PARAMETER
//  │                │
    AnnotationTarget.VALUE_PARAMETER,
//  enum class annotation/AnnotationTarget: Enum<annotation/AnnotationTarget>
//  │                enum entry annotation/AnnotationTarget.CONSTRUCTOR
//  │                │
    AnnotationTarget.CONSTRUCTOR,
//  enum class annotation/AnnotationTarget: Enum<annotation/AnnotationTarget>
//  │                enum entry annotation/AnnotationTarget.FUNCTION
//  │                │
    AnnotationTarget.FUNCTION,
//  enum class annotation/AnnotationTarget: Enum<annotation/AnnotationTarget>
//  │                enum entry annotation/AnnotationTarget.TYPE
//  │                │
    AnnotationTarget.TYPE
) annotation class base

//constructor base()
//│
@base annotation class derived

//constructor base() constructor base()
//│                  │
@base class correct(@base val x: Int) {
//   constructor base()       Int
//   │                        │
    @base constructor(): this(0)
}

//constructor base()
//│
@base enum class My {
//   constructor base()
//   │
    @base FIRST,
//   constructor base()
//   │
    @base SECOND
}

//constructor base()
//│            constructor base()
//│            │        constructor base()
//│            │        │
@base fun foo(@base y: @base Int): Int {
//   constructor base()
//   │             constructor base()
//   │             │        constructor base()
//   │             │        │         Int
//   │             │        │         │ foo.bar.z: Int
//   │             │        │         │ │ fun (Int).plus(Int): Int
//   │             │        │         │ │ │ Int
//   │             │        │         │ │ │ │
    @base fun bar(@base z: @base Int) = z + 1
//   constructor base()
//   │                fun foo.bar(Int): Int
//   │        Int     │   foo.y: Int
//   │        │       │   │
    @base val local = bar(y)
//         val foo.local: Int
//         │
    return local
}

//constructor base()
//│       Int Int
//│       │   │
@base val z = 0
