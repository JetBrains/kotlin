package test

class TopLevelClass {
    class MiddleClass {
        class NestedClass {
            fun nestedMember() = <expr>0</expr>
        }

        fun middleMember() = 1
    }

    fun topMember() = 2
}
