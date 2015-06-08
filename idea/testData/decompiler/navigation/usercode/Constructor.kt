import testData.libraries.*

val x: ClassWithConstructor = ClassWithConstructor("abc", 239)
val xx: ClassWithConstructor = ClassWithConstructor("abc")

// main.kt
//public class <1><3>ClassWithConstructor<2>(val a: String, b: Any) {
//    <4>constructor(a: String): this(a, a)
