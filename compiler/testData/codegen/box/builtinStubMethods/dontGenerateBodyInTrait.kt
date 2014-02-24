trait A : Collection<String>
trait B : List<String>
trait C : Map<Long, Double>
trait D : Map.Entry<Any, Nothing>
trait E : Iterator<Int>

fun box(): String {
    trait F : A, B, C, D, E
    return "OK"
}
