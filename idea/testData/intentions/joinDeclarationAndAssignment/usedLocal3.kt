class Test(height: Int, width: Int) {
    private val size: Int = height * width
    private val <caret>data: Int

    init {
        data = size
    }
}