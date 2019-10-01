// "Make private and overrides 'getName'" "true"
// DISABLE-ERRORS
class B : JavaClass() {
    private val name: String = ""
    override fun getName(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}