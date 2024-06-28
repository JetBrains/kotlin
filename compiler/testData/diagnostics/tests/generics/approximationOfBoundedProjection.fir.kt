// ISSUE: KT-21463

interface BoxWrapper<T, B : Box<T>> {
    val box: B
}

interface Box<T> {
    val item: T
}

fun <T> getBoxWrapper(): BoxWrapper<T, *> = null!!

class Element

val box: Box<Element> = getBoxWrapper<Element>().box
