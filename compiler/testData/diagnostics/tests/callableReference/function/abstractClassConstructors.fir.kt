// !DIAGNOSTICS: -UNUSED_EXPRESSION
interface A
abstract class B
annotation class C
enum class D

fun main() {
    ::A
    ::B
    ::C   // KT-3465
    ::D
}