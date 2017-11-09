// PROBLEM: none

class KotlinType
class KotlinTypeInfo(val type: KotlinType?) {
    fun foo(other: KotlinTypeInfo) {
        if (<caret>type != null && other.type != null) {

        }
    }
}