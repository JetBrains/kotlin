package test

public trait Super1 {
    val x: String
    var y: String
}

public trait Super2 {
    var x: String
    val y: String
}

public trait Sub: Super1, Super2 {
}
