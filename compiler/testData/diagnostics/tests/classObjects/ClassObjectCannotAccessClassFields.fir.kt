// http://youtrack.jetbrains.net/issue/KT-20

class A() {
    val x = 1

    companion object {
        val y = x
    }
}