package abc.foo

enum class Season {
    WINTER
    SPRING
    SUMMER
    AUTUMN
}

class A {
    public fun bar1(x : Season) : String {
        when (x) {
            Season.WINTER, Season.SPRING -> return "winter_spring"
            Season.SPRING -> return "spring"
            Season.SUMMER -> return "summer"
        }

        return "autumn";
    }

    public fun bar2(y : Season) : String {
        return bar3(y) { x ->
            when (x) {
                Season.WINTER, Season.SPRING -> "winter_spring"
                Season.SPRING -> "spring"
                Season.SUMMER -> "summer"
                else -> "autumn"
            }
        }
    }

    private fun bar3(x : Season, block : (Season) -> String) = block(x)
}

// 2 TABLESWITCH
// 1 @abc/foo/A\$WhenMappings\.class
