inline fun <reified T> labeledComponent(labelText: String): T = <caret>T::class.java.getConstructor().newInstance()

