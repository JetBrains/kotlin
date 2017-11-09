// FILE: test.kt

// @TestKt.class:
// 0 valueOf
// 0 Value\s\(\)

val mask = 127
val entries = IntArray(128)
val flags = BooleanArray(128)

fun distance(index: Int, hash: Int): Int = (128 + index - (hash and mask)) and mask

fun insertSad(x: Int): Boolean {
    return insertWithBoxing(
            x,
            hash = { it },
            equals = { a, b -> a == b },
            isEmpty = { !flags[it] },
            fetch = { entries[it] },
            store = { i, x -> entries[i] = x; flags[i] = true; }
    )
}

// FILE: inline.kt
inline fun <T> insertWithBoxing(entry: T,
                                hash: (T) -> Int,
                                equals: (T, T) -> Boolean,
                                isEmpty: (Int) -> Boolean,
                                fetch: (Int) -> T,
                                store: (Int, T) -> Unit): Boolean {
    var currentEntry = entry
    var index = hash(entry) and mask
    var dist = 0
    do {
        if (isEmpty(index)) {
            store(index, currentEntry)
            return true
        }

        val existingEntry = fetch(index)
        if (equals(existingEntry, currentEntry)) {
            return false
        }

        val existingHash = hash(existingEntry)
        val existingDistance = distance(index, existingHash)
        if (existingDistance < dist) {
            store(index, currentEntry)
            currentEntry = existingEntry
            dist = existingDistance
        }

        dist += 1
        index = (index + 1) and mask
    }
    while (true)
}

