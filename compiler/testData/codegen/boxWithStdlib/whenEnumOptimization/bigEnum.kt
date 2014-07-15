import kotlin.test.assertEquals

enum class BigEnum {
    ITEM1 ITEM2 ITEM3 ITEM4 ITEM5 ITEM6 ITEM7 ITEM8 ITEM9 ITEM10 ITEM11 ITEM12 ITEM13 ITEM14 ITEM15 ITEM16 ITEM17 ITEM18 ITEM19 ITEM20
}

fun bar1(x : BigEnum) : String {
    when (x) {
        BigEnum.ITEM1, BigEnum.ITEM2, BigEnum.ITEM3 -> return "123"
        BigEnum.ITEM4, BigEnum.ITEM5, BigEnum.ITEM6 -> return "456"
    }

    return "-1";

}

fun bar2(x : BigEnum) : String {
    when (x) {
        BigEnum.ITEM7, BigEnum.ITEM8, BigEnum.ITEM9 -> return "789"
        BigEnum.ITEM10 -> return "10"
        BigEnum.ITEM11, BigEnum.ITEM12 -> return "1112"
        else -> return "-1"
    }
}

fun box() : String {
    //bar1
    assertEquals("123", bar1(BigEnum.ITEM1))
    assertEquals("123", bar1(BigEnum.ITEM2))
    assertEquals("123", bar1(BigEnum.ITEM3))

    assertEquals("456", bar1(BigEnum.ITEM4))
    assertEquals("456", bar1(BigEnum.ITEM5))
    assertEquals("456", bar1(BigEnum.ITEM6))

    assertEquals("-1", bar1(BigEnum.ITEM7))

    //bar2
    assertEquals("789", bar2(BigEnum.ITEM7))
    assertEquals("789", bar2(BigEnum.ITEM8))
    assertEquals("789", bar2(BigEnum.ITEM9))

    assertEquals("10", bar2(BigEnum.ITEM10))

    assertEquals("1112", bar2(BigEnum.ITEM11))
    assertEquals("1112", bar2(BigEnum.ITEM12))

    return "OK"
}
