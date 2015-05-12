interface A : Collection<String>
interface B : List<String>
interface C : Map<Long, Double>
interface D : Map.Entry<Any, Nothing>
interface E : Iterator<Int>

fun box(): String {
    interface F : A, B, C, D, E
    return "OK"
}
