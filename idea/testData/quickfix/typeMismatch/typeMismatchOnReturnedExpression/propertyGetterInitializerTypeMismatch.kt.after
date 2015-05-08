// "Change 'A.x' type to '() -> Int'" "true"
class A {
    var x: () -> Int
        get(): () -> Int = if (true) { {42}<caret> } else { {24} }
        set(i: () -> Int) {}
}