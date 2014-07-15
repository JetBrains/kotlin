import kotlin.test.assertEquals

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

fun box() : String {
    val a = A()

    assertEquals("winter_spring", a.bar1(Season.WINTER))
    assertEquals("winter_spring", a.bar1(Season.SPRING))
    assertEquals("summer", a.bar1(Season.SUMMER))
    assertEquals("autumn", a.bar1(Season.AUTUMN))

    assertEquals("winter_spring", a.bar2(Season.WINTER))
    assertEquals("winter_spring", a.bar2(Season.SPRING))
    assertEquals("summer", a.bar2(Season.SUMMER))
    assertEquals("autumn", a.bar2(Season.AUTUMN))


    return "OK"
}
