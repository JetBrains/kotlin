@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("StringsKt")

package kotlin.text

public inline fun CharSequence.trim(predicate: (Char) -> Boolean): CharSequence {
    var startIndex = 0
    var endIndex = length - 1
    var startFound = false

    while (startIndex <= endIndex) {
        val index = if (!startFound) startIndex else endIndex
        val match = predicate(this[index])

        if (!startFound) {
            if (!match)
                startFound = true
            else
                startIndex += 1
        } else {
            if (!match)
                break
            else
                endIndex -= 1
        }
    }

    return subSequence(startIndex, endIndex + 1)
}

public fun CharSequence.trim(): CharSequence = trim(Char::isWhitespace)

public inline fun String.trim(): String = (this as CharSequence).trim().toString()

public inline fun CharSequence.isEmpty(): Boolean = length == 0

public inline fun CharSequence.isNotEmpty(): Boolean = length > 0

public operator fun CharSequence.iterator(): Iterator<Char> = object : Iterator<Char> {
    private var index = 0

    public override fun next(): Char = get(index++)

    public override fun hasNext(): Boolean = index < length
}

public fun CharSequence.first(): Char {
    if (isEmpty())
        throw NoSuchElementException("Char sequence is empty.")
    return this[0]
}

public val CharSequence.indices: IntRange
    get() = 0..length - 1

public val CharSequence.lastIndex: Int
    get() = this.length - 1

public inline fun CharSequence.substring(startIndex: Int, endIndex: Int = length): String = subSequence(startIndex, endIndex).toString()

public fun CharSequence.substring(range: IntRange): String = subSequence(range.start, range.endInclusive + 1).toString()

public inline fun CharSequence.elementAtOrElse(index: Int, defaultValue: (Int) -> Char): Char {
    return if (index >= 0 && index <= lastIndex) get(index) else defaultValue(index)
}

public fun CharSequence.toList(): List<Char> {
    return when (length) {
        0 -> emptyList()
        1 -> listOf(this[0])
        else -> this.toMutableList()
    }
}

public fun CharSequence.toMutableList(): MutableList<Char> {
    return toCollection(ArrayList<Char>(length))
}

public fun <C : MutableCollection<in Char>> CharSequence.toCollection(destination: C): C {
    for (item in this) {
        destination.add(item)
    }
    return destination
}