// FIR_IDENTICAL
interface Visitor {
    fun visit()

    fun visitArray(): Visitor? = null

    fun visitAnnotation(): Visitor? = null
}

class AnnotationLoader {
    fun loadAnnotation(): Visitor? {
        return object : Visitor {
            override fun visit() {}

            override fun visitArray(): Visitor? {
                return object : Visitor {
                    override fun visit() {
                        foo()
                    }
                }
            }

            override fun visitAnnotation(): Visitor? {
                val visitor = loadAnnotation()!!
                return object : Visitor by visitor {
                    override fun visit() {}
                }
            }

            private fun foo() {}
        }
    }
}
