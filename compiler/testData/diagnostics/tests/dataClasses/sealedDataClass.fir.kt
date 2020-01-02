sealed data class My(val x: Int) {
    object Your: My(1)
    class His(y: Int): My(y)
}
