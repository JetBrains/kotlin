// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: commonFile.kt
package p

expect class R {
    override fun equals(@EqualityBound(R::class) other: Any?): Boolean
}

expect interface ListLike {
    override fun equals(@EqualityBound(ListLike::class) other: Any?): Boolean
}

expect class ArrayListLike : ListLike {
    override fun equals(@EqualityBound(ListLike::class) other: Any?): Boolean
}

// MODULE: platform()()(common)
// FILE: q/ArrayListLike.java
package q;

public class ArrayListLike implements p.ListLike {
    public boolean equals(Object other) {
        return other instanceof p.ListLike;
    }
}

// FILE: p/platformFile.kt
package p

class Impl {
    override fun equals(@EqualityBound(R::class) other: Any?): Boolean = true
}

actual typealias R = Impl

actual interface ListLike {
    actual override fun equals(@EqualityBound(ListLike::class) other: Any?): Boolean
}

actual typealias ArrayListLike = q.ArrayListLike

/* GENERATED_FIR_TAGS: actual, classDeclaration, classReference, expect, functionDeclaration, nullableType, operator,
override, typeAliasDeclaration */
