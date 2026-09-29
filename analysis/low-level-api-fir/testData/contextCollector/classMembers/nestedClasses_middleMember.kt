package test

class TopLevelClass {
    class MiddleClass {
        class NestedClass {
            fun nestedMember() = 0
        }

        fun middleMember() = <expr>1</expr>
    }

    fun topMember() = 2
}
