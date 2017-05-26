class Extended { val extMember = 1 }

class ExtContext {
    val ctxMember = 2

    fun Extended.<caret>extend() = extMember + ctxMember

    fun call(extended: Extended) {
        val v = extended.extend()
    }
}