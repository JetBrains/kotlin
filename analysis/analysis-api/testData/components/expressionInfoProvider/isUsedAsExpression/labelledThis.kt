class C {
    fun String.test(): Int {
        return <expr>this@C</expr>.hashCode()
    }
}