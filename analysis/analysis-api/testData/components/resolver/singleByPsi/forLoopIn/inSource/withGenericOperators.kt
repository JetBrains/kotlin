package f

class MyIterator<T>(val value: T) {
    var returned = false
}

operator fun <T> MyIterator<T>.hasNext(): Boolean = !this.returned

operator fun <T> MyIterator<T>.next(): T {
    this.returned = true
    return this.value
}

class Container<T>(val item: T)

operator fun <T> Container<T>.iterator(): MyIterator<T> = MyIterator(item)

fun usage() {
    for (i <caret>in Container<String>("hello")) {

    }
}
