// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt
package test

expect enum class Direction {
    NORTH, EAST, SOUTH, WEST
}

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: Jvm.kt
package test

actual enum class Direction(val whereToGo: String) {
    NORTH("up"), EAST("right"), SOUTH("down"), WEST("left");

    private fun test() {
        cl<caret>one()
    }
}
