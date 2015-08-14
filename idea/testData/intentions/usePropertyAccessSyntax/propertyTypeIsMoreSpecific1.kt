// IS_APPLICABLE: false
// WITH_RUNTIME

abstract class KotlinClass : JavaInterface {
    override fun getSomething(): String = ""
}

fun foo(k: KotlinClass) {
    k.<caret>setSomething(1)
}

