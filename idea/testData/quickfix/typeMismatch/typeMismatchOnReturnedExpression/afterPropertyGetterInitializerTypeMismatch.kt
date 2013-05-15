// "Change 'A.x' type to '() -> Int'" "true"
class A {
    val x: () -> Int
        get(): () -> Int = if (true) { {42}<caret> } else { {24} }
}