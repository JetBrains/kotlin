class C() {
    val a: Int = 1

    default object {
        val x : Int

        {
            $x = 1
        }


        fun foo() {
            val b : Int = 1
            doSmth(b)
        }
    }
}

fun doSmth(i: Int) {}

fun test1() {
    val a = object {
        val x : Int
        {
            $x = 1
        }
    }
}

object O {
    val x : Int
    {
        $x = 1
    }
}

fun test2() {
    val b = 1
    val a = object {
        val x = b
    }
}
fun test3() {
    val a = object {
        val y : Int
        fun inner_bar() {
            y = 10
        }
    }
}
fun test4() {
    val a = object {
        val x : Int
        val y : Int
        {
            $x = 1
        }
        fun ggg() {
            y = 10
        }
    }
}

fun test5() {
    val a = object {
        var x = 1
        {
            $x = 2
        }
        fun foo() {
            x = 3
        }
        fun bar() {
            x = 4
        }
    }
}