package test

open class Base(val value: Int)

interface Marker

class TopLevelClass {
    class MiddleClass : <expr>Base(42), Marker</expr> {
        class NestedClass {
            fun nestedMember() = 0
        }

        fun middleMember() = 1
    }

    fun topMember() = 2
}
