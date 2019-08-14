interface Some

object O1 : Some

object O2 : Some

enum class SomeEnum(val x: Some) {
//       constructor SomeEnum(Some)
//       │object O1: Some
//       ││    
    FIRST(O1) {
//                                             Boolean
//                                             │
        override fun check(y: Some): Boolean = true
    },
//        constructor SomeEnum(Some)
//        │object O2: Some
//        ││     
    SECOND(O2)  {
//                                             SomeEnum.SECOND.check.y: Some
//                                             │ fun (Any).equals(Any?): Boolean
//                                             │ │  object O2: Some
//                                             │ │  │ 
        override fun check(y: Some): Boolean = y == O2
    };

    abstract fun check(y: Some): Boolean
}
