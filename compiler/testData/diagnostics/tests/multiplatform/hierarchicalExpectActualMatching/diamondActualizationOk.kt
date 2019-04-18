// !LANGUAGE: +MultiPlatformProjects
// MODULE: top
// FILE: top.kt
package foo

expect class Top


// MODULE: left(top)
// FILE: left.kt
package foo

actual class Top {
    fun fromLeft() { }
}
expect class C

// MODULE: right(top)
// FILE: right.kt
package foo

actual class Top {
    fun fromRight() { }
}
expect class C

// MODULE: bottom(left, right)
// FILE: bottom.kt
package foo

fun foo(t: Top) {
    // Reference to merged actuals: we resolve to `fromLeft`, because `left` goes before `right`
    // in dependencies list (similarly to how symbol clashes are resolved by classpath order in JVM)
    t.fromLeft()
}

actual class C // actualizes two expects simultaneously


