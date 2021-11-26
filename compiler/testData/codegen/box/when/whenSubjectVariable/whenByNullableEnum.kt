// !LANGUAGE: +VariableDeclarationInWhenSubject
// WITH_STDLIB

import kotlin.test.assertEquals

enum class Season {
    WINTER,
    SPRING,
    SUMMER,
    AUTUMN
}

fun foo1(x : Season?) : String {
    when(val xx = x) {
        Season.AUTUMN, Season.SPRING -> return "autumn_or_spring $xx";
        Season.SUMMER, null -> return "summer_or_null $xx"
    }

    return "other"
}

fun foo2(x : Season?) : String {
    when(val xx = x) {
        Season.AUTUMN, Season.SPRING -> return "autumn_or_spring $xx";
        Season.SUMMER -> return "summer"
    }

    return "other"
}

fun box() : String {
    assertEquals("autumn_or_spring AUTUMN", foo1(Season.AUTUMN))
    assertEquals("autumn_or_spring SPRING", foo1(Season.SPRING))
    assertEquals("summer_or_null SUMMER", foo1(Season.SUMMER))
    assertEquals("summer_or_null null", foo1(null))

    assertEquals("autumn_or_spring AUTUMN", foo2(Season.AUTUMN))
    assertEquals("autumn_or_spring SPRING", foo2(Season.SPRING))
    assertEquals("summer", foo2(Season.SUMMER))
    assertEquals("other", foo2(null))

    return "OK"
}
