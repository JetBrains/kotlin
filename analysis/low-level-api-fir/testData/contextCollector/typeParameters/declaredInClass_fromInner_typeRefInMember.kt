package test

class Outer<MyParam> {
    inner class Inner {
        fun member(p: <expr>MyParam</expr>) {}
    }
}