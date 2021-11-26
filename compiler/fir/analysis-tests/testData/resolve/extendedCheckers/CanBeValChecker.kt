// WITH_STDLIB

import kotlin.reflect.KProperty
import kotlin.properties.Delegates

fun testDelegator() {
    var <!UNUSED_VARIABLE!>x<!>: Boolean by LocalFreezableVar(true)
    var <!UNUSED_VARIABLE!>y<!> by LocalFreezableVar("")
}

class LocalFreezableVar<T>(private var value: T)  {
    operator fun getValue(thisRef: Nothing?, property: KProperty<*>): T  = value

    operator fun setValue(thisRef: Nothing?, property: KProperty<*>, value: T) {
        this.value = value
    }
}


operator fun C.plus(a: Any): C = this
operator fun C.plusAssign(a: Any) {}

fun testOperatorAssignment() {
    val c = C()
    c += ""
    <!CAN_BE_VAL!>var<!> c1 = C()
    <!ASSIGN_OPERATOR_AMBIGUITY!>c1 += ""<!>

    var a = 1
    a += 12
    <!ASSIGNED_VALUE_IS_NEVER_READ!>a<!> -= 10
}


fun destructuringDeclaration() {
    <!CAN_BE_VAL!>var<!> (v1, <!UNUSED_VARIABLE!>v2<!>) = getPair()
    print(v1)

    var (v3, <!VARIABLE_NEVER_READ!>v4<!>) = getPair()
    print(v3)
    <!ASSIGNED_VALUE_IS_NEVER_READ!>v4<!> = ""

    var (<!VARIABLE_NEVER_READ!>v5<!>, <!UNUSED_VARIABLE!>v6<!>) = getPair()
    <!ASSIGNED_VALUE_IS_NEVER_READ!>v5<!> = 1

    var (<!VARIABLE_NEVER_READ!>v7<!>, <!VARIABLE_NEVER_READ!>v8<!>) = getPair()
    <!ASSIGNED_VALUE_IS_NEVER_READ!>v7<!> = 2
    <!ASSIGNED_VALUE_IS_NEVER_READ!>v8<!> = "42"

    val (<!UNUSED_VARIABLE!>a<!>, <!UNUSED_VARIABLE!>b<!>, <!UNUSED_VARIABLE!>c<!>) = Triple(1, 1, 1)

    <!CAN_BE_VAL!>var<!> (<!UNUSED_VARIABLE!>x<!>, <!UNUSED_VARIABLE!>y<!>, <!UNUSED_VARIABLE!>z<!>) = Triple(1, 1, 1)
}

fun stackOverflowBug() {
    <!CAN_BE_VAL!>var<!> <!VARIABLE_NEVER_READ!>a<!>: Int
    <!ASSIGNED_VALUE_IS_NEVER_READ!>a<!> = 1
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
    var <!VARIABLE_NEVER_READ!>v1<!> = 1
    var v2 = 2
    <!CAN_BE_VAL!>var<!> v3 = 3
    <!ASSIGNED_VALUE_IS_NEVER_READ!>v1<!> = 1
    <!ASSIGNED_VALUE_IS_NEVER_READ!>v2<!>++ // todo mark this UNUSED_CHANGED_VALUES
    print(v3)
}

fun test() {
    var a = 0
    while (a>0) {
        a++
    }
}

fun foo() {
    <!CAN_BE_VAL!>var<!> <!VARIABLE_NEVER_READ!>a<!>: Int
    val bool = true
    if (bool) <!ASSIGNED_VALUE_IS_NEVER_READ!>a<!> = 4 else <!ASSIGNED_VALUE_IS_NEVER_READ!>a<!> = 42
    val <!VARIABLE_NEVER_READ!>b<!>: String

    <!ASSIGNED_VALUE_IS_NEVER_READ!>b<!> = <!ASSIGNMENT_TYPE_MISMATCH!>false<!>
}

fun cycles() {
    var a = 10
    while (a > 0) {
        a--
    }

    var <!VARIABLE_NEVER_READ!>b<!>: Int
    while (a < 10) {
        a++
        <!ASSIGNED_VALUE_IS_NEVER_READ!>b<!> = a
    }
}

fun assignedTwice(p: Int) {
    var <!VARIABLE_NEVER_READ!>v<!>: Int
    <!ASSIGNED_VALUE_IS_NEVER_READ!>v<!> = 0
    if (p > 0) <!ASSIGNED_VALUE_IS_NEVER_READ!>v<!> = 1
}

fun main(args: Array<String?>) {
    <!CAN_BE_VAL!>var<!> a: String?
    val <!UNUSED_VARIABLE!>unused<!> = 0

    if (args.size == 1) {
        <!ASSIGNED_VALUE_IS_NEVER_READ!>a<!> = args[0]
    } else {
        a  = args.toString()
        if (<!SENSELESS_COMPARISON!>a != null<!> && a.equals("cde")) return
    }
}

fun run(f: () -> Unit) = f()

fun lambda() {
    var <!VARIABLE_NEVER_READ!>a<!>: Int
    <!ASSIGNED_VALUE_IS_NEVER_READ!>a<!> = 10

    run {
        <!ASSIGNED_VALUE_IS_NEVER_READ!>a<!> = 20
    }
}

fun lambdaInitialization() {
    <!CAN_BE_VAL!>var<!> <!VARIABLE_NEVER_READ!>a<!>: Int

    run {
        <!ASSIGNED_VALUE_IS_NEVER_READ!>a<!> = 20
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
    var <!VARIABLE_NEVER_READ!>s<!>: String by Delegates.notNull()
    <!ASSIGNED_VALUE_IS_NEVER_READ!>s<!> = ""
}
