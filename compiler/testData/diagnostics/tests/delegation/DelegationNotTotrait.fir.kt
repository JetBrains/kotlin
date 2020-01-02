open class Foo() {

}

class Barrr() : Foo by Foo() {}

interface T {}

class Br(t : T) : T by t {}

open enum class EN() {
  A
}

class Test2(e : EN) : EN by e {}
