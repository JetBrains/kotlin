package test

public interface Super1 {
    val x: String
    val y: CharSequence
}

public interface Super2 {
    val x: CharSequence
    val y: String
}

public interface Sub: Super1, Super2 {
}
