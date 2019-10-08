abstract class Base(val s: String)

class Outer {
//                             constructor Base(String)
//                             │    Outer.Derived.<init>.s: String
//                             │    │
    class Derived(s: String) : Base(s)

//               constructor Base(String)
//               │
    object Obj : Base("")
}
