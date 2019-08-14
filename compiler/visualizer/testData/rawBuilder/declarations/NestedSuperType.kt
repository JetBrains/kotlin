package p

abstract class My {
//                             constructor My()
//                             │
    abstract class NestedOne : My() {
//                                 constructor My.NestedOne()
//                                 │
        abstract class NestedTwo : NestedOne() {

        }
    }
}

//           constructor My()
//           │
class Your : My() {
//                      constructor My.NestedOne()
//                      │
    class NestedThree : NestedOne()
}
