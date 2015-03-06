package ceAnonymousObject

public val publicTopLevelObject: Any = object { fun test() = 1 }
private val privateTopLevelObject = object { fun test() = 1 }

fun main(args: Array<String>) {
    MyClass().foo()
}

class MyClass {
    public val publicObject: Any = object { fun test() = 1 }
    protected val protectedObject: Any = object { fun test() = 1 }
    private val privateObject = object { fun test() = 1 }

    fun foo() {
        val localObject = object { fun test() = 1 }
        //Breakpoint!
        val a = 1
    }
}

// EXPRESSION: publicTopLevelObject
// RESULT: instance of ceAnonymousObject.CeAnonymousObjectPackage$ceAnonymousObject$@packagePartHASH$publicTopLevelObject$1(id=ID): LceAnonymousObject/CeAnonymousObjectPackage$ceAnonymousObject$@packagePartHASH$publicTopLevelObject$1;

// EXPRESSION: privateTopLevelObject
// RESULT: instance of ceAnonymousObject.CeAnonymousObjectPackage$ceAnonymousObject$@packagePartHASH$privateTopLevelObject$1(id=ID): LceAnonymousObject/CeAnonymousObjectPackage$ceAnonymousObject$@packagePartHASH$privateTopLevelObject$1;

// EXPRESSION: publicObject
// RESULT: instance of ceAnonymousObject.MyClass$publicObject$1(id=ID): LceAnonymousObject/MyClass$publicObject$1;

// EXPRESSION: protectedObject
// RESULT: instance of ceAnonymousObject.MyClass$protectedObject$1(id=ID): LceAnonymousObject/MyClass$protectedObject$1;

// -EXPRESSION: privateObject
// -RESULT: 1

// -EXPRESSION: privateObject.test()
// -RESULT: 1: I

// EXPRESSION: localObject
// RESULT: instance of ceAnonymousObject.MyClass$foo$localObject$1(id=ID): LceAnonymousObject/MyClass$foo$localObject$1;

// EXPRESSION: localObject.test()
// RESULT: 1: I

