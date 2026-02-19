// IGNORE_FE10
// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt
package test

expect enum class Direction {
    NORTH, EAST, SOUTH, WEST
}

// MODULE: js()()(common)
// TARGET_PLATFORM: JS
// FILE: Js.kt
package test

actual enum class Direction(val whereToGo: String) {
    NORTH("up"), EAST("right"), SOUTH("down"), WEST("left");

    private fun test() {
        cl<caret>one()
    }
}