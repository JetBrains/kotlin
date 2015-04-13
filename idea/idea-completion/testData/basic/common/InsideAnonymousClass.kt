class A(private val xxx1: Int, val xxx2: Int) {
    private val xxx3: Int = 0

    private val x = object : Runnable {
        override fun run() {
            <caret>
        }
    }
}

// EXIST: xxx1
// EXIST: xxx2
// EXIST: xxx3
// EXIST: x
