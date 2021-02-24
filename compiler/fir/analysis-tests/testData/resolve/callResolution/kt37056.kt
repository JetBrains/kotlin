fun case1(a: A?) {
    val test = a?.let {

        Case1.invoke(it)

        Case1(it)

        Case1(A())
    }

    Case1(A())
    Case1(a = A())
}

class Case1 private constructor(val a: A) {
    companion object {
        operator fun invoke(a: A) = ""
    }
}

fun case2(a: A) {
    Case2(a)
    Case2(a =a)
}

class Case2 {
    companion object {
        operator fun invoke(a: A) = "" //(1)
    }
}

class A()