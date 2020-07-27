// WITH_RUNTIME

import kotlin.reflect.KProperty
import kotlin.properties.Delegates

fun testDelegator() {
    var x: Boolean by LocalFreezableVar(true)
    var y by LocalFreezableVar("")
}

class LocalFreezableVar<T>(private var value: T)  {
    operator fun getValue(thisRef: Nothing?, property: KProperty<*>): T  = value

    operator fun setValue(thisRef: Nothing?, property: KProperty<*>, value: T) {
        this.value = value
    }
}


class C
operator fun C.plus(a: Any): C = this
operator fun C.plusAssign(a: Any) {}

fun testOperatorAssignment() {
    val c = C()
    c += ""
    <!CAN_BE_VAL!>var<!> c1 = C()
    <!ASSIGN_OPERATOR_AMBIGUITY!>c1 += ""<!>

    var a = 1
    a += 12
    a -= 10
}


fun destructuringDeclaration() {
    <!CAN_BE_VAL!>var<!> (v1, v2) = getPair()
    print(v1)

    var (v3, v4) = getPair()
    print(v3)
    v4 = ""

    var (v5, v6) = getPair()
    v5 = 1

    var (v7, v8) = getPair()
    v7 = 2
    v8 = "42"

    val (a, b, c) = Triple(1, 1, 1)

    <!CAN_BE_VAL!>var<!> (x, y, z) = Triple(1, 1, 1)
}

fun stackOverflowBug() {
    <!CAN_BE_VAL!>var<!> a: Int
    a = 1
    for (i in 1..10)
        print(i)
}

fun smth(flag: Boolean) {
    var a = 1

    if (flag) {
        while (a > 0) {
            a--
        }
    }
}

fun withAnnotation(p: List<Any>) {
    @Suppress("UNCHECKED_CAST")
    <!CAN_BE_VAL!>var<!> v = p as List<String>
    print(v)
}

fun withReadonlyDeligate() {
    val s: String by lazy { "Hello!" }
    s.hashCode()
}

fun getPair(): Pair<Int, String> = Pair(1, "1")

fun listReceiver(p: List<String>) {}

fun withInitializer() {
    var v1 = 1
    var v2 = 2
    <!CAN_BE_VAL!>var<!> v3 = 3
    v1 = 1
    v2++
    print(v3)
}

fun test() {
    var a = 0
    while (a>0) {
        a++
    }
}

fun foo() {
    <!CAN_BE_VAL!>var<!> a: Int
    val bool = true
    val b: String

    if (bool) a = 4 else a = 42

    bool = false
}

fun cycles() {
    var a = 10
    while (a > 0) {
        a--
    }

    val b: Int
    while (a < 10) {
        a++
        b = a
    }
}

fun assignedTwice(p: Int) {
    var v: Int
    v = 0
    if (p > 0) v = 1
}

fun main(args: Array<String?>) {
    <!CAN_BE_VAL!>var<!> a: String?

    if (args.size == 1) {
        a = args[0]
    }
    else {
        a  = args.toString()
    }

    if (a != null && a.equals("cde")) return
}

fun run(f: () -> Unit) = f()

fun lambda() {
    var a: Int
    a = 10

    run {
        a = 20
    }
}

fun lambdaInitialization() {
    <!CAN_BE_VAL!>var<!> a: Int

    run {
        a = 20
    }
}

fun notAssignedWhenNotUsed(p: Int) {
    <!CAN_BE_VAL!>var<!> v: Int
    if (p > 0) {
        v = 1
        print(v)
    }
}

var global = 1

class C {
    var field = 2

    fun foo() {
        print(field)
        print(global)
    }
}

fun withDelegate() {
    var s: String by Delegates.notNull()
    s = ""
}
