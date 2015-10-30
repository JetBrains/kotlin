package test

public enum class X { A, B }

public inline fun switch(x: X): String = when (x) {
    X.A -> "O"
    X.B -> "K"
}