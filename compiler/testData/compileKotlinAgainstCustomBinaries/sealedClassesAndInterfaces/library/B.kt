package test

sealed class B : Base(), IBase {
    class First : B()
    class Second : B()
}
