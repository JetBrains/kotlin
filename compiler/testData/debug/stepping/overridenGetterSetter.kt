// IGNORE_BACKEND: WASM
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
// test.kt:31 box
// test.kt:16 <init>
// test.kt:18 <init>
// test.kt:21 <init>
// EXPECTATIONS JVM_IR
// test.kt:16 <init>
// EXPECTATIONS JVM JVM_IR
// test.kt:31 box
// test.kt:32 box
// test.kt:16 testPropertyInInterface
// test.kt:9 testPropertyInInterface
// test.kt:18 getPropVal2
// test.kt:9 testPropertyInInterface
// test.kt:10 testPropertyInInterface
// test.kt:21 getPropVar2
// test.kt:10 testPropertyInInterface
// test.kt:11 testPropertyInInterface
// test.kt:21 setPropVar2
// test.kt:12 testPropertyInInterface
// test.kt:16 testPropertyInInterface
// test.kt:33 box
// test.kt:24 testPropertyInInterfaceImpl
// test.kt:18 getPropVal2
// test.kt:24 testPropertyInInterfaceImpl
// test.kt:25 testPropertyInInterfaceImpl
// test.kt:21 getPropVar2
// test.kt:25 testPropertyInInterfaceImpl
// test.kt:26 testPropertyInInterfaceImpl
// test.kt:21 setPropVar2
// test.kt:27 testPropertyInInterfaceImpl
// test.kt:34 box

// EXPECTATIONS JS_IR
// test.kt:31 box
// test.kt:18 <init>
// test.kt:21 <init>
// test.kt:16 <init>
// test.kt:32 box
// test.kt:9 testPropertyInInterface
// test.kt:18 <get-propVal2>
// test.kt:10 testPropertyInInterface
// test.kt:21 <get-propVar2>
// test.kt:11 testPropertyInInterface
// test.kt:21 <set-propVar2>
// test.kt:21 <set-propVar2>
// test.kt:12 testPropertyInInterface
// test.kt:33 box
// test.kt:26 testPropertyInInterfaceImpl
// test.kt:27 testPropertyInInterfaceImpl
// test.kt:34 box
