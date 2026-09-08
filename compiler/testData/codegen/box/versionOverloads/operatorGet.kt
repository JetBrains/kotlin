@file:OptIn(ExperimentalVersionOverloading::class)

class VersionedGrid {
    private var value: Int = 0

    operator fun get(
        row: Int,
        column: Int,
        @IntroducedAt("1") layer: Int = 0,
    ): Int = row + column + layer + value
}

fun box(): String {
    val grid = VersionedGrid()
    if (grid[1, 2] != 3) return "FAIL get default"
    if (grid[1, 2, 3] != 6) return "FAIL get full"
    return "OK"
}
