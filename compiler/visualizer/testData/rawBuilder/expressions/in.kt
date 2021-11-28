// FIR_IGNORE
//                         collections/Collection<Int>
//                         │                Boolean
//                         │                │
fun foo(x: Int, y: Int, c: Collection<Int>) =
//  foo.x: Int
//  │ fun (collections/Collection<E>).contains(E): Boolean
//  │ │  foo.c: collections/Collection<Int>
//  │ │  │    foo.y: Int
//  │ │  │    │ fun (collections/Collection<E>).contains(E): Boolean
//  │ │  │    │ │   foo.c: collections/Collection<Int>
//  │ │  │    │ │   │
    x in c && y !in c
