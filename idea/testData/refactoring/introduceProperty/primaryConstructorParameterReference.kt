// EXTRACTION_TARGET: property with initializer
class Foo(s: String) {
    val l = <selection>(s + "1")</selection>.length
}