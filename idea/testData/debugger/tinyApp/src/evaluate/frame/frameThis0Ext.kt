package frameThis0Ext

fun main(args: Array<String>) {
    A().test()
}

class A {
    val prop1 = 1

    fun AExt.testExt() {
        val val1 = 1
        foo {
            //Breakpoint!
            prop1 + prop2 + val1
        }
    }

    fun test() {
        AExt().testExt()
    }
}

class AExt {
    val prop2 = 1
}

fun foo(f: () -> Unit) {
    f()
}

// PRINT_FRAME