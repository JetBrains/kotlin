// "Make private and implements 'setName'" "true"
// DISABLE-ERRORS
class A(private var name: String) : JavaInterface {
    override fun setName(name: String?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getName(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}