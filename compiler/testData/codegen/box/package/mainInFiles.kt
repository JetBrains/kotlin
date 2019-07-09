// TARGET_BACKEND: JVM

// WITH_RUNTIME
// FILE: 1.kt

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



// FILE: a.kt

package pkg

fun main(args: Array<String>) {
    args[0] += "O"
}

// FILE: b.kt

package pkg

fun main(args: Array<String>) {
    args[0] += "K"
}
