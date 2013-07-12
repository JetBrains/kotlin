package test

public trait Super1 {
    val x: String
    val y: CharSequence
}

public trait Super2 {
    val x: CharSequence
    val y: String
}

public trait Sub: Super1, Super2 {
}
