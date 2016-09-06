
fun topLevelFunction(){

}

object B1 {
    @JvmStatic
    fun staticMethodOfObject() {
    }

    fun nonStaticMethodOfObject() {
    }
}

class C1 {
    fun methodOfClass() {
    }

    companion object {
        @JvmStatic
        fun staticMethodOfCompanion() {
        }

        fun nonStaticMethodOfCompanion() {
        }
    }
}
