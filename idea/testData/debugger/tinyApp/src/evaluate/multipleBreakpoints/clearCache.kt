package clearCache

import java.util.ArrayList
import java.util.HashSet

fun primitiveTypes() {
    if (true) {
        val a: Any = if (1 == 1) 0 else "abc"
        // EXPRESSION: a
        // RESULT: instance of java.lang.Integer(id=ID): Ljava/lang/Integer;
        //Breakpoint!
        val b = 1
    }

    if (true) {
        val a = 1
        // EXPRESSION: a
        // RESULT: 1: I
        //Breakpoint!
        val b = 1
    }

    for (i in 1..2) {
        // EXPRESSION: i
        // RESULT: 1: I

        // EXPRESSION: i
        // RESULT: 2: I

        //Breakpoint!
        val b = 1
    }

    for (i in 'a'..'b') {
        // EXPRESSION: i
        // RESULT: 97: C

        // EXPRESSION: i
        // RESULT: 98: C

        //Breakpoint!
        val b = 1
    }
}

fun subType() {
    if (true) {
        val o = Base()
        // EXPRESSION: o.test()
        // RESULT: 100: I
        //Breakpoint!
        val b = 1
    }

    if (true) {
        val o = Derived()
        // EXPRESSION: o.test()
        // RESULT: 200: I
        //Breakpoint!
        val b = 1
    }
}

open class Base {
    open fun test() = 100
}

class Derived: Base() {
    override fun test() = 200
}

fun subTypePlatform() {
    if (true) {
        val c: MutableList<String> = ArrayList<String>()
        // EXPRESSION: c.size()
        // RESULT: 0: I
        //Breakpoint!
        val b = 1
    }

    if (true) {
        val c: List<String> = ArrayList<String>()
        // EXPRESSION: c.size()
        // RESULT: 0: I
        //Breakpoint!
        val b = 1
    }

    if (true) {
        val c = ArrayList<String>()
        c.add("a")
        // EXPRESSION: c.size()
        // RESULT: 1: I
        //Breakpoint!
        val b = 1
    }

    if (true) {
        val c = ArrayList<Int>()
        c.add(1)
        c.add(2)
        // EXPRESSION: c.size()
        // RESULT: 2: I
        //Breakpoint!
        val b = 1
    }

    if (true) {
        val c = HashSet<Int>()
        // EXPRESSION: c.size()
        // RESULT: 0: I
        //Breakpoint!
        val b = 1
    }
}

fun innerClass() {
    if (true) {
        val o = TestInnerClasses.Base()
        // EXPRESSION: o.test()
        // RESULT: 100: I
        //Breakpoint!
        val b = 1
    }

    if (true) {
        val o = TestInnerClasses.Derived()
        // EXPRESSION: o.test()
        // RESULT: 200: I
        //Breakpoint!
        val b = 1
    }
}

class TestInnerClasses {
    open class Base {
        open fun test() = 100
    }

    class Derived: Base() {
        override fun test() = 200
    }
}

fun objects() {
    // EXPRESSION: obj.test()
    // RESULT: 1: I
    //Breakpoint!
    val a1 = 1

    // EXPRESSION: obj.test()
    // RESULT: 1: I
    //Breakpoint!
    val a2 = 1

    if (true) {
        val o: BaseObject = obj
        // EXPRESSION: o.test()
        // RESULT: 1: I
        //Breakpoint!
        val b = 1
    }

    if (true) {
        val o = obj
        // EXPRESSION: o.test()
        // RESULT: 1: I
        //Breakpoint!
        val b = 1
    }
}

internal val obj = object: BaseObject() { }

open class BaseObject {
    fun test() = 1
}

fun main(args: Array<String>) {
    primitiveTypes()
    subType()
    subTypePlatform()
    innerClass()
    objects()
}
