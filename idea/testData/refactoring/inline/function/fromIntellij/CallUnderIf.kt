class Foo {
    fun getComponent(i: Int?): String? {
        return null
    }

    var myI: Int? = null

    fun usage() {
        if (myI != null)
            method(myI)
    }

    fun <caret>method(i: Int?) {
        println(getComponent(myI)!! + getComponent(i)!!)
    }

}