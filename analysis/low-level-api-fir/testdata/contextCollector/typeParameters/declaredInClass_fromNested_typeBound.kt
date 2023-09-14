package test

class Outer<MyParam> {
    // TODO this test has an error, MyParam should not be resolved here. See KT-61959
    class Nested<T : <expr>MyParam</expr>>
}