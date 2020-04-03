// !DUMP_CFG
fun test_1() {
    try {
        val x = 1
    } catch (e: RuntimeException) {
        val y = 2
    } catch (e: Exception) {
        val z = 3
    }
}

fun test_2() {
    val x = try {
        1
    } catch (e: Exception) {
        2
    }
}

fun test_3(b: Boolean) {
    while (true) {
        try {
            if (b) return
            val x = 1
            if (!b) break
        } catch (e: Exception) {
            continue
        } catch (e: RuntimeException) {
            break
        }
        val y = 2
    }
    val z = 3
}