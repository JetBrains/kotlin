open class A<T : Any>(val javaClass: Class<T>)

class B : A<String>(<caret>)

// EXIST_JAVA_ONLY: { lookupString: "String::class.java", itemText: "String::class.java", attributes: "" }
