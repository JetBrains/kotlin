
fun box() : String {
    val result1 : Int = (1..100).filter { x -> x % 2 == 0 }.size()
    if (result1 != 50) return "fail 1: {$result1}"

    val result2 : Int = (1..100).map { x -> (2 * x) }.filter { x -> x % 2 == 0 }.size()
    if (result2 != 100) return "fail 2: {$result2}";


    return "OK"
}
