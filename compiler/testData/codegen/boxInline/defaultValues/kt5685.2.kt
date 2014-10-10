package test

class Measurements
{
    inline fun measure(key: String, logEvery: Long = -1, divisor: Int = 1, body: () -> Unit): String {
        body()
        return key
    }
}