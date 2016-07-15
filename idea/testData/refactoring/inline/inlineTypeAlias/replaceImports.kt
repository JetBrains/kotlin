package a

import c.C

class A<X>

typealias <caret>R<X> = A<C<X>>

typealias I = R<String>