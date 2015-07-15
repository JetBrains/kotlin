package dependency

interface Iterable<T>
interface List<T> : Iterable<T>

fun <T> List<T>.xxx(t: T){}
fun <T> Iterable<T>.xxx(t: T){}
