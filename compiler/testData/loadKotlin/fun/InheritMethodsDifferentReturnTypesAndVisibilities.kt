package test

public trait Super1 {
    public fun foo(): CharSequence
    private fun bar(): String
}

public trait Super2 {
    private fun foo(): String
    public fun bar(): CharSequence
}

public trait Sub: Super1, Super2 {
}
