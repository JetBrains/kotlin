// "Make private and implements 'setName'" "true"
// DISABLE-ERRORS
class A(private var name: String) : JavaInterface {
    override fun setName(name: String?) {
        TODO("Not yet implemented")
    }

    override fun getName(): String {
        TODO("Not yet implemented")
    }
}