class Outer {/* NonReanalyzableClassDeclarationStructureElement */
    val i: Int = 1/* ReanalyzablePropertyStructureElement */
        get() {
            class Inner {
                var i: Int = 2
                    get() {
                        field++
                        return field
                    }
                val j: Int = 3
                    get() {
                        field = 42
                        return field
                    }

                fun innerMember() {
                    field++
                }
            }
            return field
        }

    val j: Int = 4/* ReanalyzablePropertyStructureElement */
        get() {
            fun local() {
                field++
                field++
            }
            local()
            return field
        }
}
