package test

class TopLevelClass {
    class MiddleClass {
        class NestedClass {
            fun nestedMember() = 0
        }

        fun middleMember() = 1
    }

    fun topMember() = <expr>2</expr>
}
