import kotlin.collections.map as map1
import kotlin.Array as KotlinArray

fun f() {
    listOf(1).map1 { it.hashCode() }
    listOf(1).map { it.hashCode() }
}

fun g(a1: KotlinArray<Int>, a2: Array<Int>){}