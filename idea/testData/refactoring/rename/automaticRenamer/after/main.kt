open class Bar : Throwable()

val BAR: Bar = Bar()
val BAR_1: Bar = Bar()

val BARs: List<Bar> = listOf()
val FOOS_1: Array<Bar> = array()

fun main(args: Array<String>) {
    val bar: Bar = Bar()
    val someVerySpecialBar: Bar = Bar()
    val barAnother: Bar = Bar()

    val anonymous = object : Bar() {
    }

    val (bar1: Bar, bars: List<Bar>) = Pair(Bar(), listOf<Bar>())

    try {
        for (bar2: Bar in listOf<Bar>()) {

        }
    } catch (bar: Bar) {

    }

    fun local(bar: Bar) {

    }
}

fun topLevel(bar: Bar) {

}

fun collectionLikes(bars: List<Array<Bar>>, foos: List<Map<Bar, Bar>>) {

}

class BarImpl : Bar()

object BarObj : Bar()