// FILE: Context.java

public interface Context {
    String BEAN = "context";
}

// FILE: Test.kt

annotation class Resource(val name: String)

class MyController {
    companion object {
        private const val foo = Context.BEAN
    }

    @Resource(name = Context.BEAN)
    fun setContext() {
    }
}