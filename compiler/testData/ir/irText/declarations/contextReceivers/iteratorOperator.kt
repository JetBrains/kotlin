// !LANGUAGE: +ContextReceivers
// WITH_STDLIB
data class Counter(var i: Int = 0)

data class CounterConfig(val max: Int = 10)

context(CounterConfig)
class CounterIterator(private val counter: Counter) : Iterator<Int> {
    override fun hasNext() = counter.i < max
    override fun next() = counter.i++
}

context(CounterConfig)
operator fun Counter.iterator() = with(this@CounterConfig) { CounterIterator(this@Counter) }


fun box(): String {
    var result = 0
    with(CounterConfig()) {
        for (i in Counter()) {
            result += i
        }
    }
    return if (result == 45) "OK" else "fail"
}
