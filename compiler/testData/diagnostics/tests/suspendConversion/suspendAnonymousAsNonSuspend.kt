// ISSUE: KT-58055

fun <T> produce(arg: () -> T): T = arg()

fun main() {
    produce {
        suspend fun() {} // CCE
    }
}
