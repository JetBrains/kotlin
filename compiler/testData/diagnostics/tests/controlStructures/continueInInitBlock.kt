// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-47892

fun test(b: Boolean)  {
    while (b) {
        class A {
            init {
                continue
            }
            constructor(): super()
        }
    }
}
