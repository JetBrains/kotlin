// "Make private and overrides 'getName'" "true"
// DISABLE-ERRORS
class B : JavaClass() {
    private val name: String = ""
    override fun getName(): String {
        TODO("Not yet implemented")
    }
}