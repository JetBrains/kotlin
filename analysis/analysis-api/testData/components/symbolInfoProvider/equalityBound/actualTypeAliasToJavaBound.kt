// LANGUAGE: +StrictEquals +MultiPlatformProjects
// function: impl/JavaListLike.equals(other)

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt
package api

expect interface ListLike {
    override fun equals(@EqualityBound(ListLike::class) other: Any?): Boolean
}

expect class JavaListLike : ListLike {
    override fun equals(@EqualityBound(ListLike::class) other: Any?): Boolean
}

// MODULE: main()()(common)
// TARGET_PLATFORM: JVM
// FILE: impl/JavaListLike.java
package impl;

public class JavaListLike implements api.ListLike {
    @Override
    public boolean equals(Object other) {
        return other instanceof api.ListLike;
    }
}

// FILE: platform.kt
package api

actual interface ListLike {
    actual override fun equals(@EqualityBound(ListLike::class) other: Any?): Boolean
}

actual typealias JavaListLike = impl.JavaListLike
