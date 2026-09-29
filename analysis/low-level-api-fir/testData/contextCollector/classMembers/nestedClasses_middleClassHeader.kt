package test

open class Base(val value: Int)

class TopLevelClass {
    class MiddleClass : Base(<expr>42</expr>) {
        class NestedClass {
            fun nestedMember() = 0
        }

        fun middleMember() = 1
    }

    fun topMember() = 2
}
