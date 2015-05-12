package test

public interface Super1 {
    val x: String
    var y: String
}

public interface Super2 {
    var x: String
    val y: String
}

public interface Sub: Super1, Super2 {
}
