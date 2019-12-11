val flag = true

interface I
class A(): I
class B(): I

val a = l@ {
    return@l if (flag) A() else B()
}