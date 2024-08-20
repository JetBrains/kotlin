abstract class AbstractOpenDefault {
    open val a: String? = ""
}

abstract class AbstractAbstractDefault : AbstractOpenDefault(){
    abstract override val a: String?
}

abstract class AbstractFinalDefault : AbstractOpenDefault() {
    final override val a: String? = ""
}

abstract class AbstractOpenCustom : AbstractOpenDefault(){
    open override val a: String?
        get() = ""
}

abstract class AbstractFinalCustom: AbstractOpenDefault() {
    final override val a: String?
        get() = ""
}

abstract class AbstractAbstractFake : AbstractAbstractDefault()

abstract class AbstractOpenFake : AbstractOpenDefault()

abstract class AbstractOpenFakeCustom : AbstractOpenCustom()

abstract class AbstractFinalFake: AbstractFinalDefault()

abstract class  AbstractFinalFakeCustom: AbstractFinalCustom()

open class OpenOpenDefault: AbstractOpenDefault() {
    open override val a: String? = ""
}

open class OpenFinalDefault: AbstractOpenDefault() {
    final override val a: String? = ""
}

open class OpenOpenCustom: AbstractOpenDefault() {
    open override val a: String?
        get() = ""
}

open class OpenFinalCustom: AbstractOpenDefault() {
    final override val a: String?
        get() = ""
}

open class OpenOpenFake: OpenOpenDefault()

open class OpenOpenFakeCustom: OpenOpenCustom()

open class OpenFinalFake : OpenFinalDefault()

open class OpenFinalFakeCustom : OpenFinalCustom()

class FinalOpenDefault: AbstractOpenDefault() {
    <!NON_FINAL_MEMBER_IN_FINAL_CLASS!>open<!> override val a: String? = ""
}

class FinalFinalDefault: AbstractOpenDefault() {
    final override val a: String? = ""
}

class FinalOpenCustom: AbstractOpenDefault() {
    <!NON_FINAL_MEMBER_IN_FINAL_CLASS!>open<!> override val a: String?
        get() = ""
}

class FinalFinalCustom: AbstractOpenDefault() {
    final override val a: String?
        get() = ""
}

class FinalOpenFake: AbstractOpenDefault()

class FinalOpenFakeCustom: AbstractOpenCustom()

class FinalFinalFake : AbstractFinalDefault()

class FinalFinalFakeCustom : AbstractFinalCustom()

fun test1(a: AbstractOpenDefault) {
    if(a is AbstractAbstractDefault){
        a.a as String
        <!SMARTCAST_IMPOSSIBLE!>a.a<!>.length
    }
    if(a is AbstractFinalDefault){
        a.a as String
        a.a.length
    }
    if(a is AbstractOpenCustom){
        a.a as String
        <!SMARTCAST_IMPOSSIBLE!>a.a<!>.length
    }
    if(a is AbstractFinalCustom){
        a.a as String
        <!SMARTCAST_IMPOSSIBLE!>a.a<!>.length
    }
    if(a is AbstractAbstractFake){
        a.a as String
        <!SMARTCAST_IMPOSSIBLE!>a.a<!>.length
    }
    if(a is AbstractOpenFake){
        a.a as String
        <!SMARTCAST_IMPOSSIBLE!>a.a<!>.length
    }
    if(a is AbstractOpenFakeCustom){
        a.a as String
        <!SMARTCAST_IMPOSSIBLE!>a.a<!>.length
    }
    if(a is AbstractFinalFake){
        a.a as String
        a.a.length
    }
    if(a is AbstractFinalFakeCustom){
        a.a as String
        <!SMARTCAST_IMPOSSIBLE!>a.a<!>.length
    }
    if(a is OpenOpenDefault){
        a.a as String
        <!SMARTCAST_IMPOSSIBLE!>a.a<!>.length
    }
    if(a is OpenFinalDefault){
        a.a as String
        a.a.length
    }
    if(a is OpenOpenCustom){
        a.a as String
        <!SMARTCAST_IMPOSSIBLE!>a.a<!>.length
    }
    if(a is OpenFinalCustom){
        a.a as String
        <!SMARTCAST_IMPOSSIBLE!>a.a<!>.length
    }
    if(a is OpenOpenFake){
        a.a as String
        <!SMARTCAST_IMPOSSIBLE!>a.a<!>.length
    }
    if(a is OpenOpenFakeCustom){
        a.a as String
        <!SMARTCAST_IMPOSSIBLE!>a.a<!>.length
    }
    if(a is OpenFinalFake){
        a.a as String
        a.a.length
    }
    if(a is OpenFinalFakeCustom){
        a.a as String
        <!SMARTCAST_IMPOSSIBLE!>a.a<!>.length
    }
    if(a is FinalOpenDefault){
        a.a as String
        a.a.length
    }
    if(a is FinalFinalDefault){
        a.a as String
        a.a.length
    }
    if(a is FinalOpenCustom){
        a.a as String
        <!SMARTCAST_IMPOSSIBLE!>a.a<!>.length
    }
    if(a is FinalFinalCustom){
        a.a as String
        <!SMARTCAST_IMPOSSIBLE!>a.a<!>.length
    }
    if(a is FinalOpenFake){
        a.a as String
        a.a.length
    }
    if(a is FinalOpenFakeCustom){
        a.a as String
        <!SMARTCAST_IMPOSSIBLE!>a.a<!>.length
    }
    if(a is FinalFinalFake){
        a.a as String
        a.a.length
    }
    if(a is FinalFinalFakeCustom){
        a.a as String
        <!SMARTCAST_IMPOSSIBLE!>a.a<!>.length
    }
}

fun test2() {
    var a: AbstractOpenDefault = null!!
    a.a as String
    <!SMARTCAST_IMPOSSIBLE!>a.a<!>.length

    a = OpenOpenDefault()
    a.a as String
    <!SMARTCAST_IMPOSSIBLE!>a.a<!>.length

    a = OpenFinalDefault()
    a.a as String
    a.a.length

    a = OpenFinalCustom()
    a.a as String
    <!SMARTCAST_IMPOSSIBLE!>a.a<!>.length

    a = OpenOpenCustom()
    a.a as String
    <!SMARTCAST_IMPOSSIBLE!>a.a<!>.length

    a = OpenOpenFake()
    a.a as String
    <!SMARTCAST_IMPOSSIBLE!>a.a<!>.length

    a = OpenFinalFake()
    a.a as String
    a.a.length

    a = OpenFinalFakeCustom()
    a.a as String
    <!SMARTCAST_IMPOSSIBLE!>a.a<!>.length

    a = FinalOpenDefault()
    a.a as String
    a.a.length

    a = FinalFinalDefault()
    a.a as String
    a.a.length

    a = FinalOpenCustom()
    a.a as String
    <!SMARTCAST_IMPOSSIBLE!>a.a<!>.length

    a = FinalFinalCustom()
    a.a as String
    <!SMARTCAST_IMPOSSIBLE!>a.a<!>.length

    a = FinalOpenFake()
    a.a as String
    a.a.length

    a = FinalFinalFakeCustom()
    a.a as String
    <!SMARTCAST_IMPOSSIBLE!>a.a<!>.length

    a = FinalFinalFake()
    a.a as String
    a.a.length

    a = FinalOpenFakeCustom()
    a.a as String
    <!SMARTCAST_IMPOSSIBLE!>a.a<!>.length
}
