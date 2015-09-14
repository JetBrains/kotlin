class A0 @Ann (x: Int) {
    val x = 1
}

class A1 Ann : Base()

class A2 Ann

class A3 Ann {
    fun foo()
}

class A4 constructor {}
class A5 constructor : Base {}

class A7 @Ann(1) (x: Int)
class A8 @Ann() {}
class A9 @Ann() : Base()
