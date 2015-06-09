public     data   open  class    FooClass1    {

}

public    open
         data      class    FooClass2

//-----------------------

public    fun     fooFun1   () {}

public
fun     fooFun2   () {}

//-----------------------
public    fun     Int    .   extFun1   () {}

public
fun     Int
        .
        extFun2   () {}
//-----------------------
    val     fooVal1   =  1

val     fooVal2   =  1
//-----------------------
    var     fooVar1   =  1

var     fooVar2   =   2
//-----------------------
private    val     Int    .   extVal1   =  122

private
val
        Int
        .
        extVal     :    Int     =    1
//-----------------------
public    var     Int    .   extVar1:Int   =  122

public
var
        Int
        .
        extVar     :    Int     =    1
//-----------------------
public    var     varWithAccessors1:Int
    get()    { return  1 }
    set  (value    :  Int)  { /**/ }

public       var         varWithAccessors2:    Int
    get()  {
    1
}
    set(value: Int)
    {
        /**/
    }
}
//-----------------------
annotation class A1
annotation class A2

private  @[   A1   A2   A1 ]   A1    A2  @[   A1  A2   A2     ]   @[A1] val     fooProp1   = 1

private  @[


A1


A2   A1 ]   A1    A2  @[A1
A2




A2

]   @[A1] val     fooProp1   = 1

private  A1

A2    val
        fooProp2   = 1


//-----------------------

public   object    FooObject1   {

}

public
object    FooObject2   {

}