package test

class A {}

fun getMain(className: String): java.lang.reflect.Method {
    val classLoader = A().javaClass.classLoader
    return classLoader.loadClass(className).getDeclaredMethod("main", Array<String>::class.java)
}

fun box(): String {
    val bMain = getMain("pkg.AKt")
    val cMain = getMain("pkg.BKt")

    val args = Array(1, { "" })

    bMain.invoke(null, args)
    cMain.invoke(null, args)

    return args[0]
}

