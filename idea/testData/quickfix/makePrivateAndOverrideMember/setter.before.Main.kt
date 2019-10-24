// "Make private and implements 'setName'" "true"
// DISABLE-ERRORS
class A(<caret>var name: String) : JavaInterface {
    override fun getName(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}