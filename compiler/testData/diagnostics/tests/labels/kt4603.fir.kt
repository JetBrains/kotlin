// !DIAGNOSTICS: -UNUSED_VARIABLE
//KT-4603 Labeling information is lost when passing through local classes or objects

fun foo() {
    val s: Int.() -> Unit = l@{
        class Local(val y: Int = this@l) {
            fun bar() {
                val x: Int = this@l //unresolved
            }
        }
    }
}
