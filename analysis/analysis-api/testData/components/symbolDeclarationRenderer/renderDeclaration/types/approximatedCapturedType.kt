// WITH_STDLIB

class SimpleClass
val classloader = SimpleClass::class.java.classLoader
val obj = classloader?.loadClass("hi")?.kotlin