// FILE: test.kt

interface MyInterfaceWithoutBreakpoints {
    val propVal2: Int
    var propVar2: Int

    fun testPropertyInInterface() {
        propVal2
        propVar2
        propVar2 = 2
    }
}

// Breakpoint at GETFILED/PUTFIELD
class MyInterfaceImplWithBreakpoints : MyInterfaceWithoutBreakpoints {
    //FieldWatchpoint! (propVal2)
    override val propVal2 = 1

    //FieldWatchpoint! (propVar2)
    override var propVar2 = 1

    fun testPropertyInInterfaceImpl() {
        propVal2
        propVar2
        propVar2 = 2
    }
}

fun box() {
    val macwbi = MyInterfaceImplWithBreakpoints()
    macwbi.testPropertyInInterface()
    macwbi.testPropertyInInterfaceImpl()
}


// EXPECTATIONS JVM JVM_IR
// test.kt:30 box
// test.kt:15 <init>
// test.kt:17 <init>
// test.kt:20 <init>
// EXPECTATIONS JVM_IR
// test.kt:15 <init>
// EXPECTATIONS JVM JVM_IR
// test.kt:30 box
// test.kt:31 box
// test.kt:15 testPropertyInInterface
// test.kt:8 testPropertyInInterface
// test.kt:17 getPropVal2
// test.kt:8 testPropertyInInterface
// test.kt:9 testPropertyInInterface
// test.kt:20 getPropVar2
// test.kt:9 testPropertyInInterface
// test.kt:10 testPropertyInInterface
// test.kt:20 setPropVar2
// test.kt:11 testPropertyInInterface
// test.kt:15 testPropertyInInterface
// test.kt:32 box
// test.kt:23 testPropertyInInterfaceImpl
// test.kt:17 getPropVal2
// test.kt:23 testPropertyInInterfaceImpl
// test.kt:24 testPropertyInInterfaceImpl
// test.kt:20 getPropVar2
// test.kt:24 testPropertyInInterfaceImpl
// test.kt:25 testPropertyInInterfaceImpl
// test.kt:20 setPropVar2
// test.kt:26 testPropertyInInterfaceImpl
// test.kt:33 box

// EXPECTATIONS JS_IR
// test.kt:30 box
// test.kt:17 <init>
// test.kt:20 <init>
// test.kt:15 <init>
// test.kt:31 box
// test.kt:8 testPropertyInInterface
// test.kt:17 <get-propVal2>
// test.kt:9 testPropertyInInterface
// test.kt:20 <get-propVar2>
// test.kt:10 testPropertyInInterface
// test.kt:20 <set-propVar2>
// test.kt:20 <set-propVar2>
// test.kt:11 testPropertyInInterface
// test.kt:32 box
// test.kt:25 testPropertyInInterfaceImpl
// test.kt:26 testPropertyInInterfaceImpl
// test.kt:33 box
